/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
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
import com.rebuild.web.EntityController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 用户设置
 *
 * @author devezhao
 * @since 10/08/2018
 */
@RestController
@RequestMapping("/settings")
public class UserSettingsController extends EntityController {

    @GetMapping("/user")
    public ModelAndView pageUser(HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/settings/user-settings");

        User user = Application.getUserStore().getUser(getRequestUser(request));
        mv.getModelMap().put("user", user);

        String dingtalkCorpid = RebuildConfiguration.get(ConfigurationItem.DingtalkCorpid);
        if (dingtalkCorpid != null) {
            Object[] dingtalkUser = Application.createQueryNoFilter(
                    "select appUser from ExternalUser where bindUser = ? and appId = ?")
                    .setParameter(1, user.getId())
                    .setParameter(2, dingtalkCorpid)
                    .unique();
            if (dingtalkUser != null) mv.getModelMap().put("dingtalkUser", dingtalkUser[0]);
        }
        String wxworkCorpid = RebuildConfiguration.get(ConfigurationItem.WxworkCorpid);
        if (wxworkCorpid != null) {
            Object[] wxworkUser = Application.createQueryNoFilter(
                    "select appUser from ExternalUser where bindUser = ? and appId = ?")
                    .setParameter(1, user.getId())
                    .setParameter(2, wxworkCorpid)
                    .unique();
            if (wxworkUser != null) mv.getModelMap().put("wxworkUser", wxworkUser[0]);
        }

        return mv;
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

        if (sentid != null) {
            return RespBody.ok();
        } else {
            return RespBody.errorl("操作失败，请稍后重试");
        }
    }

    @RequestMapping("/user/save-email")
    public RespBody saveEmail(HttpServletRequest request) {
        ID user = getRequestUser(request);
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

    @RequestMapping("/user/save-passwd")
    public RespBody savePasswd(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        String oldp = getParameterNotNull(request, "oldp");
        String newp = getParameterNotNull(request, "newp");

        Object[] o = Application.createQuery("select password from User where userId = ?")
                .setParameter(1, user)
                .unique();
        if (o == null || !StringUtils.equals((String) o[0], EncryptUtils.toSHA256Hex(oldp))) {
            return RespBody.errorl("原密码输入有误");
        }

        return savePasswd(user, newp);
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
        int appType = getIntParameter(request, "type", 0);
        // 1=Dingtalk, 2=Wxwork
        String appId = appType == 1
                ? RebuildConfiguration.get(ConfigurationItem.DingtalkCorpid)
                : RebuildConfiguration.get(ConfigurationItem.WxworkCorpid);

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
}
