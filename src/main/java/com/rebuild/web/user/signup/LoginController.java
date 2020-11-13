/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user.signup;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.api.user.LoginToken;
import com.rebuild.core.Application;
import com.rebuild.core.ServerStatus;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.VerfiyCode;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.utils.AES;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.wf.captcha.utils.CaptchaUtil;
import eu.bitwalker.useragentutils.UserAgent;
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
import java.util.List;
import java.util.Map;

/**
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
@RestController
@RequestMapping("/user/")
public class LoginController extends BaseController {

    public static final String CK_AUTOLOGIN = "rb.alt";

    private static final String SK_NEED_VCODE = "needLoginVCode";

    private static final String DEFAULT_HOME = "../dashboard/home";

    @GetMapping("login")
    public ModelAndView checkLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (AppUtils.getRequestUser(request) != null) {
            response.sendRedirect(DEFAULT_HOME);
            return null;
        }

        // 授权 Token 登录
        final String useToken = getParameter(request, "token");
        if (StringUtils.isNotBlank(useToken)) {
            ID tokenUser = AuthTokenManager.verifyToken(useToken, true);
            if (tokenUser != null) {
                loginSuccessed(request, response, tokenUser, false);

                String nexturl = StringUtils.defaultIfBlank(request.getParameter("nexturl"), DEFAULT_HOME);
                response.sendRedirect(CodecUtils.urlDecode(nexturl));
                return null;
            } else {
                // 立即显示验证码
                ServletUtils.setSessionAttribute(request, SK_NEED_VCODE, true);
            }
        }

        // Cookie 记住登录
        final String useAlt = ServletUtils.readCookie(request, CK_AUTOLOGIN);
        if (StringUtils.isNotBlank(useAlt)) {
            ID altUser = null;
            try {
                String[] alts = AES.decrypt(useAlt).split(",");
                altUser = ID.isId(alts[0]) ? ID.valueOf(alts[0]) : null;

                // 最大一个月有效期
                if (altUser != null) {
                    long t = ObjectUtils.toLong(alts[1]);
                    if ((System.currentTimeMillis() - t) / 1000 > 30 * 24 * 60 * 60) {
                        altUser = null;
                    }
                }

            } catch (Exception ex) {
                ServletUtils.readCookie(request, CK_AUTOLOGIN);
                LOG.error("Cannot decode User from alt : " + useAlt, ex);
            }

            if (altUser != null && Application.getUserStore().existsUser(altUser)) {
                loginSuccessed(request, response, altUser, true);

                String nexturl = StringUtils.defaultIfBlank(request.getParameter("nexturl"), DEFAULT_HOME);
                response.sendRedirect(CodecUtils.urlDecode(nexturl));
                return null;
            } else {
                // 显示验证码
                ServletUtils.setSessionAttribute(request, SK_NEED_VCODE, true);
            }
        }

        // 登录页
        ModelAndView mv = createModelAndView("/signup/login");

        // 切换语言
        putLocales(mv, AppUtils.getReuqestLocale(request));

        return mv;
    }

    @PostMapping("user-login")
    public RespBody userLogin(HttpServletRequest request, HttpServletResponse response) {
        String vcode = getParameter(request, "vcode");
        Boolean needVcode = (Boolean) ServletUtils.getSessionAttribute(request, SK_NEED_VCODE);
        if (needVcode != null && needVcode
                && (StringUtils.isBlank(vcode) || !CaptchaUtil.ver(vcode, request))) {
            return RespBody.errorl("SomeError", "Captcha");
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

        User loginUser = Application.getUserStore().getUser(user);
        loginSuccessed(request, response, loginUser.getId(), getBoolParameter(request, "autoLogin", false));

        // 清理
        getLoginRetryTimes(user, -1);
        ServletUtils.setSessionAttribute(request, SK_NEED_VCODE, null);

        String danger = ServerStatus.checkValidity();
        if (danger != null) {
            return RespBody.ok(JSONUtils.toJSONObject("danger", danger));
        } else {
            return RespBody.ok();
        }
    }

    /**
     * @param user
     * @param state
     * @return
     */
    private int getLoginRetryTimes(String user, int state) {
        String key = "LoginRetry-" + user;
        if (state == -1) {
            Application.getCommonsCache().evict(key);
            return 0;
        }

        Integer retry = (Integer) Application.getCommonsCache().getx(key);
        retry = retry == null ? 0 : retry;
        if (state == 1) {
            retry += 1;
            Application.getCommonsCache().putx(key, retry, CommonsCache.TS_HOUR);
        }
        return retry;
    }

    /**
     * 登录成功
     *
     * @param request
     * @param response
     * @param user
     * @param autoLogin
     */
    private void loginSuccessed(HttpServletRequest request, HttpServletResponse response, ID user, boolean autoLogin) {
        // 自动登录
        if (autoLogin) {
            String alt = user + "," + System.currentTimeMillis() + ",v1";
            alt = AES.encrypt(alt);
            ServletUtils.addCookie(response, CK_AUTOLOGIN, alt);
        } else {
            ServletUtils.removeCookie(request, response, CK_AUTOLOGIN);
        }

        createLoginLog(request, user);

        ServletUtils.setSessionAttribute(request, WebUtils.CURRENT_USER, user);
        Application.getSessionStore().storeLoginSuccessed(request);
    }

    /**
     * 创建登陆日志
     *
     * @param request
     * @param user
     */
    protected void createLoginLog(HttpServletRequest request, ID user) {
        String ipAddr = ServletUtils.getRemoteAddr(request);
        String UA = request.getHeader("user-agent");
        if (AppUtils.isRbMobile(request)) {
            UA = UA.toUpperCase();
        } else {
            UserAgent uas = UserAgent.parseUserAgentString(UA);
            try {
                UA = String.format("%s-%s (%s)",
                        uas.getBrowser(), uas.getBrowserVersion().getMajorVersion(), uas.getOperatingSystem());
            } catch (Exception ex) {
                LOG.warn("Unknown user-agent : " + UA);
                UA = "UNKNOW";
            }
        }

        Record record = EntityHelper.forNew(EntityHelper.LoginLog, UserService.SYSTEM_USER);
        record.setID("user", user);
        record.setString("ipAddr", ipAddr);
        record.setString("userAgent", UA);
        record.setDate("loginTime", CalendarUtils.now());
        Application.getCommonsService().create(record);
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
            return RespBody.errorl("EmailAccountUnset");
        }

        String email = getParameterNotNull(request, "email");
        if (!RegexUtils.isEMail(email) || !Application.getUserStore().existsEmail(email)) {
            return RespBody.errorl("SomeInvalid,Email");
        }

        String vcode = VerfiyCode.generate(email, 2);
        String subject = getLang(request, "ResetPassword");
        String content = String.format(getLang(request, "YourVCode", "ResetPassword"), vcode);
        String sentid = SMSender.sendMail(email, subject, content);

        if (sentid != null) {
            return RespBody.ok();
        } else {
            return RespBody.errorl("OperationFailed");
        }
    }

    @PostMapping("user-confirm-passwd")
    public RespBody userConfirmPasswd(HttpServletRequest request) {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        String newpwd = data.getString("newpwd");
        String email = data.getString("email");
        String vcode = data.getString("vcode");

        if (!VerfiyCode.verfiy(email, vcode, true)) {
            return RespBody.errorl("SomeInvalid", "Captcha");
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

        JSONObject ret = License.siteApi("api/misc/bgimg", true);
        if (ret == null) {
            return RespBody.ok();
        } else {
            return RespBody.ok(ret.getString("url"));
        }
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
