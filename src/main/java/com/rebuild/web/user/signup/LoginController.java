/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user.signup;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.api.user.LoginToken;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.*;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.utils.AES;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.user.UserAvatar;
import com.wf.captcha.utils.CaptchaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Zixin (RB)
 * @since 07/25/2018
 */
@Slf4j
@RestController
@RequestMapping("/user/")
public class LoginController extends LoginAction {

    @GetMapping("login")
    public ModelAndView checkLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String homeUrl = "../dashboard/home";
        if (AppUtils.getRequestUser(request) != null) {
            response.sendRedirect(homeUrl);
            return null;
        }

        // Token 登录
        final String useToken = getParameter(request, "token");
        if (StringUtils.isNotBlank(useToken)) {
            ID tokenUser = AuthTokenManager.verifyToken(useToken, true);
            if (tokenUser != null) {
                loginSuccessed(request, response, tokenUser, false);

                String nexturl = getParameter(request, "nexturl", homeUrl);
                if (nexturl.startsWith("http")) nexturl = homeUrl;

                response.sendRedirect(CodecUtils.urlDecode(nexturl));
                return null;
            } else {
                // 立即显示验证码
                ServletUtils.setSessionAttribute(request, SK_NEED_VCODE, true);
            }
        }

        // 记住登录
        final String useAlt = ServletUtils.readCookie(request, CK_AUTOLOGIN);
        if (StringUtils.isNotBlank(useAlt)) {
            ID altUser = null;
            try {
                String[] alts = AES.decrypt(useAlt).split(",");
                altUser = alts.length == 3 && ID.isId(alts[0]) ? ID.valueOf(alts[0]) : null;

                // 最大30天有效期
                if (altUser != null) {
                    long t = ObjectUtils.toLong(alts[1]);
                    if ((System.currentTimeMillis() - t) / 1000 > 30 * 24 * 60 * 60) {
                        altUser = null;
                    }
                    if (altUser != null && !UserHelper.isActive(altUser)) {
                        altUser = null;
                    }
                }

            } catch (Exception ex) {
                ServletUtils.removeCookie(request, response, CK_AUTOLOGIN);
                log.error("Cannot decode User from alt : {}", useAlt, ex);
            }

            if (altUser != null && Application.getUserStore().existsUser(altUser)) {
                Integer ed = loginSuccessed(request, response, altUser, true);

                String nexturl = getParameter(request, "nexturl", homeUrl);
                if (nexturl.startsWith("http")) nexturl = homeUrl;

                if (ed != null) {
                    nexturl = "../settings/passwd-expired?d=" + ed;
                }

                response.sendRedirect(CodecUtils.urlDecode(nexturl));
                return null;
            } else {
                // 立即显示验证码
                ServletUtils.setSessionAttribute(request, SK_NEED_VCODE, true);
            }
        }

        // 登录页
        ModelAndView mv = createModelAndView("/signup/login");

        // 切换语言
        putLocales(mv, AppUtils.getReuqestLocale(request));

        // 验证码
        if (RebuildConfiguration.getInt(ConfigurationItem.LoginCaptchaPolicy) == 2) {
            ServletUtils.setSessionAttribute(request, SK_NEED_VCODE, true);
        }

        // H5
        String mobileUrl = RebuildConfiguration.getMobileUrl("/");
        mv.getModel().put("mobileUrl", mobileUrl);

        if (License.isCommercial()) {
            // DingTalk
            mv.getModel().put("ssoDingtalk", RebuildConfiguration.get(ConfigurationItem.DingtalkAppkey));
            // WxWork
            mv.getModel().put("ssoWxwork", RebuildConfiguration.get(ConfigurationItem.WxworkCorpid));
        } else {
            mv.getModel().put("ssoDingtalk", "#");
            mv.getModel().put("ssoWxwork", "#");
        }

        mv.getModelMap().put("UsersMsg", SysbaseDiagnosis.getUsersDanger());
        return mv;
    }

    @PostMapping("user-login")
    public RespBody userLogin(HttpServletRequest request, HttpServletResponse response) {
        String vcode = getParameter(request, "vcode");
        Boolean needVcode = (Boolean) ServletUtils.getSessionAttribute(request, SK_NEED_VCODE);
        if ((needVcode != null && needVcode) || StringUtils.isNotBlank(vcode)) {
            if (StringUtils.isBlank(vcode)) {
                ServletUtils.setSessionAttribute(request, SK_NEED_VCODE, true);
                return RespBody.error("VCODE");
            } else if (!CaptchaUtil.ver(vcode, request)) {
                return RespBody.errorl("验证码错误");
            }
        }

        final String user = getParameterNotNull(request, "user");
        final String password = ServletUtils.getRequestString(request);

        int retry = getLoginRetryTimes(user, 1);
        if (retry > 3 && StringUtils.isBlank(vcode)) {
            ServletUtils.setSessionAttribute(request, SK_NEED_VCODE, true);
            return RespBody.error("VCODE");
        }

        String hasError = LoginToken.checkUser(user, password);
        if (hasError != null) {
            return RespBody.error(hasError);
        }

        // 清理验证码
        getLoginRetryTimes(user, -1);
        ServletUtils.setSessionAttribute(request, SK_NEED_VCODE, null);
        // 头像缓存
        ServletUtils.setSessionAttribute(request, UserAvatar.SK_DAVATAR, System.currentTimeMillis());

        final User loginUser = Application.getUserStore().getUser(user);
        final boolean isMobile = AppUtils.isRbMobile(request);

        Map<String, Object> resMap = new HashMap<>();

        // 2FA
        int faMode = RebuildConfiguration.getInt(ConfigurationItem.Login2FAMode);
        if (faMode > 0 && !UserHelper.isSuperAdmin(loginUser.getId())) {
            resMap.put("login2FaMode", faMode);

            String userToken = CodecUtils.randomCode(40);
            Application.getCommonsCache().putx("2FA" + userToken, loginUser.getId(), CommonsCache.TS_HOUR / 4); // 15m
            resMap.put("login2FaUserToken", userToken);

            if (isMobile) {
                request.getSession().invalidate();
            }

            return RespBody.ok(resMap);
        }

        if (isMobile) {
            resMap = loginSuccessedH5(request, response, loginUser.getId());
        } else {
            Integer ed = loginSuccessed(
                    request, response, loginUser.getId(), getBoolParameter(request, "autoLogin", false));
            if (ed != null) resMap.put("passwdExpiredDays", ed);
        }

        return RespBody.ok(resMap);
    }

    private int getLoginRetryTimes(String user, int state) {
        final String ckey = "LoginRetry-" + user;
        if (state == -1) {
            Application.getCommonsCache().evict(ckey);
            return 0;
        }

        Integer retry = (Integer) Application.getCommonsCache().getx(ckey);
        retry = retry == null ? 0 : retry;
        if (state == 1) {
            retry += 1;
            Application.getCommonsCache().putx(ckey, retry, CommonsCache.TS_HOUR);
        }
        return retry;
    }

    @GetMapping("logout")
    public ModelAndView logout(HttpServletRequest request, HttpServletResponse response) {
        ServletUtils.removeCookie(request, response, CK_AUTOLOGIN);
        ServletUtils.getSession(request).invalidate();
        return new ModelAndView("redirect:/user/login");
    }

    // --

    @GetMapping("forgot-passwd")
    public ModelAndView forgotPasswd() {
        return createModelAndView("/signup/forgot-passwd");
    }

    @PostMapping("user-forgot-passwd")
    public RespBody userForgotPasswd(HttpServletRequest request) {
        if (!SMSender.availableMail()) {
            return RespBody.errorl("邮件服务账户未配置，请联系管理员配置");
        }

        String email = getParameterNotNull(request, "email");
        if (!RegexUtils.isEMail(email) || !Application.getUserStore().existsEmail(email)) {
            return RespBody.errorl("无效邮箱地址");
        }

        String vcode = VerfiyCode.generate(email, 2);
        String subject = Language.L("重置密码");
        String content = Language.L("你的重置密码验证码是 : **%s**", vcode);
        String sentid = SMSender.sendMail(email, subject, content);

        if (sentid != null) {
            return RespBody.ok();
        } else {
            return RespBody.errorl("操作失败，请稍后重试");
        }
    }

    @PostMapping("user-confirm-passwd")
    public RespBody userConfirmPasswd(HttpServletRequest request) {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        String newpwd = data.getString("newpwd");
        String email = data.getString("email");
        String vcode = data.getString("vcode");

        if (!VerfiyCode.verfiy(email, vcode, true)) {
            return RespBody.errorl("无效验证码");
        }

        User user = Application.getUserStore().getUserByEmail(email);
        Record record = EntityHelper.forUpdate(user.getId(), user.getId());
        record.setString("password", newpwd);
        try {
            UserContextHolder.setUser(user.getId());

            Application.getBean(UserService.class).update(record);
            VerfiyCode.clean(email);

            return RespBody.ok();

        } catch (DataSpecificationException ex) {
            return RespBody.error(ex.getLocalizedMessage());
        } finally {
            UserContextHolder.clear();
        }
    }

    @GetMapping("live-wallpaper")
    public RespBody getLiveWallpaper() {
        if (!RebuildConfiguration.getBool(ConfigurationItem.LiveWallpaper)) {
            return RespBody.ok();
        }

        JSONObject ret = License.siteApi("api/misc/bgimg");
        if (ret == null) {
            return RespBody.ok();
        } else {
            return RespBody.ok(ret.getString("url"));
        }
    }

    @GetMapping("site-register")
    public void reg(HttpServletResponse response) throws IOException {
        response.sendRedirect("https://getrebuild.com/market/site-register?sn=" + License.SN());
    }

    // --

    /**
     * 可用语言
     *
     * @param into
     * @param currentLocale
     */
    public static void putLocales(ModelAndView into, String currentLocale) {
        String currentLocaleText = null;

        List<String[]> alangs = new ArrayList<>();
        for (Map.Entry<String, String> lc : Application.getLanguage().availableLocales().entrySet()) {
            String lcText = lc.getValue();
            lcText = lcText.split("\\(")[0].trim();

            alangs.add(new String[] { lc.getKey(), lcText });

            if (lc.getKey().equals(currentLocale)) {
                currentLocaleText = lcText;
            }
        }

        into.getModelMap().put("currentLang", currentLocaleText);
        into.getModelMap().put("availableLangs", alangs);
    }
}
