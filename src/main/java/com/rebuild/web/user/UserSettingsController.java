/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.Controller;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.DefinedException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.VerfiyCode;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.web.BaseController;
import com.rebuild.web.user.signup.LoginAction;
import com.rebuild.web.user.signup.LoginController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * 用户设置
 *
 * @author devezhao
 * @since 10/08/2018
 */
@RestController
@RequestMapping("/settings")
public class UserSettingsController extends BaseController {

    @GetMapping("/user")
    public ModelAndView pageUser(HttpServletRequest request) {
        throwIfTempAuth(request);
        final ID user = getRequestUser(request);

        ModelAndView mv = createModelAndView("/settings/user-settings");

        User ub = Application.getUserStore().getUser(user);
        mv.getModelMap().put("user", ub);

        String dingtalkCorpid = RebuildConfiguration.get(ConfigurationItem.DingtalkCorpid);
        if (dingtalkCorpid != null) {
            Object dingtalkUser = getExternalUserId(ub.getId(), dingtalkCorpid);
            if (dingtalkUser != null) mv.getModelMap().put("dingtalkUser", dingtalkUser);
        }
        String wxworkCorpid = RebuildConfiguration.get(ConfigurationItem.WxworkCorpid);
        if (wxworkCorpid != null) {
            Object wxworkUser = getExternalUserId(ub.getId(), wxworkCorpid);
            if (wxworkUser != null) mv.getModelMap().put("wxworkUser", wxworkUser);
        }
        String feishuAppid = RebuildConfiguration.get(ConfigurationItem.FeishuAppId);
        if (feishuAppid != null) {
            Object feishuUser = getExternalUserId(ub.getId(), feishuAppid);
            if (feishuUser != null) mv.getModelMap().put("feishuUser", feishuUser);
        }

        return mv;
    }

    private Object getExternalUserId(ID user, String appid) {
        Object[] o = Application.createQueryNoFilter(
                "select appUser from ExternalUser where bindUser = ? and appId = ?")
                .setParameter(1, user)
                .setParameter(2, appid)
                .unique();
        return o == null ? null : o[0];
    }

    @RequestMapping("/user/send-email-vcode")
    public RespBody sendEmailVcode(HttpServletRequest request) {
        if (!SMSender.availableMail()) {
            return RespBody.errorl("邮件服务账户未配置，请联系管理员配置");
        }

        String email = getParameterNotNull(request, "email");
        if (Application.getUserStore().existsEmail(email)) {
            return RespBody.errorl("邮箱已被占用，请换用其他邮箱");
        }

        String vcode = VerfiyCode.generate(email);
        String subject = Language.L("邮箱验证码");
        String content = Language.L("你的邮箱验证码是 : **%s**", vcode);
        String sentid = SMSender.sendMail(email, subject, content);

        if (sentid != null) return RespBody.ok();
        return RespBody.errorl("操作失败，请稍后重试");
    }

    @RequestMapping("/user/save-email")
    public RespBody saveEmail(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        String email = getParameterNotNull(request, "email");
        String vcode = getParameterNotNull(request, "vcode");

        if (!VerfiyCode.verfiy(email, vcode)) {
            return RespBody.errorl("验证码无效");
        }

        if (Application.getUserStore().existsEmail(email)) {
            return RespBody.errorl("邮箱已被占用，请换用其他邮箱");
        }

        Record record = EntityHelper.forUpdate(user, user);
        record.setString("email", email);
        Application.getBean(UserService.class).update(record);
        return RespBody.ok();
    }

    @PostMapping("/user/save-passwd")
    public RespBody savePasswd(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);

        JSONObject p = (JSONObject) ServletUtils.getRequestJson(request);
        String oldp = p.getString("oldp");
        String newp = p.getString("newp");

        Object[] o = Application.getQueryFactory().uniqueNoFilter(user, "password");
        if (o == null || !StringUtils.equals((String) o[0], EncryptUtils.toSHA256Hex(oldp))) {
            return RespBody.errorl("原密码输入有误");
        }

        RespBody res = savePasswd(user, newp);
        if (res.getErrorCode() == Controller.CODE_OK) {
            try {
                ServletUtils.removeCookie(request, response, LoginAction.CK_AUTOLOGIN);
                request.getSession().invalidate();
            } catch (Exception ignored) {}
        }
        return res;
    }

    @GetMapping("/user/login-logs")
    public Object[][] loginLogs(HttpServletRequest request) {
        Object[][] logs = Application.createQueryNoFilter(
                "select loginTime,ipAddr,userAgent from LoginLog where user = ? order by loginTime desc")
                .setParameter(1, getRequestUser(request))
                .setLimit(100)
                .array();

        for (Object[] o : logs) {
            o[0] = I18nUtils.formatDate((Date) o[0]);
        }
        return logs;
    }

    @GetMapping("/passwd-expired")
    public ModelAndView pagePasswdExpired() {
        return createModelAndView("/settings/passwd-expired");
    }

    @PostMapping("/passwd-expired-save")
    public RespBody passwdExpiredSave(@RequestBody JSONObject post, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        String newpasswd = post.getString("newpasswd");

        Object[] oldpasswd = Application.getQueryFactory().uniqueNoFilter(user, "password");
        if (oldpasswd[0].equals(EncryptUtils.toSHA256Hex(newpasswd))) {
            return RespBody.errorl("新密码与原密码不能相同");
        }

        return savePasswd(user, newpasswd);
    }

    private RespBody savePasswd(ID user, String password) {
        Record record = EntityHelper.forUpdate(user, user);
        record.setString("password", password);
        try {
            Application.getBean(UserService.class).update(record);
        } catch (DataSpecificationException ex) {
            return RespBody.error(ex.getMessage());
        }
        return RespBody.ok();
    }

    @PostMapping("/cancel-external-user")
    public RespBody cancelExternalUser(HttpServletRequest request) {
        int appType = getIntParameter(request, "type");
        // 1=Dingtalk, 2=Wxwork, 3=Feishu
        String appId = appType == 1
                ? RebuildConfiguration.get(ConfigurationItem.DingtalkCorpid)
                : RebuildConfiguration.get(ConfigurationItem.WxworkCorpid);
        if (appType == 3) appId = RebuildConfiguration.get(ConfigurationItem.FeishuAppId);

        Object[] externalUser = Application.createQueryNoFilter(
                "select userId from ExternalUser where bindUser = ? and appId = ?")
                .setParameter(1, getRequestUser(request))
                .setParameter(2, appId)
                .unique();
        if (externalUser != null) {
            Application.getCommonsService().delete((ID) externalUser[0]);
        }

        return RespBody.ok();
    }

    @PostMapping("/user/temp-auth")
    public RespBody tempAuth(HttpServletRequest request) {
        throwIfTempAuth(request);
        final ID user = getRequestUser(request);
        final String token = CodecUtils.randomCode(40);
        Application.getCommonsCache().putx(LoginController.SK_TEMP_AUTH + token, user, 60 * 5);

        String url = RebuildConfiguration.getHomeUrl("/user/login/temp-auth?token=" + token);
        return RespBody.ok(url);
    }

    private void throwIfTempAuth(HttpServletRequest request) {
        Object tempAuth = ServletUtils.getSessionAttribute(request, LoginController.SK_TEMP_AUTH);
        if (tempAuth != null) throw new DefinedException(Language.L("无权访问该页面"));
    }
}
