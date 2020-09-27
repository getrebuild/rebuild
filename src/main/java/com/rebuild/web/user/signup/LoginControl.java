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
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.api.user.LoginToken;
import com.rebuild.core.Application;
import com.rebuild.core.ServerStatus;
import com.rebuild.core.UserContext;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.*;
import com.rebuild.utils.AES;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.wf.captcha.utils.CaptchaUtil;
import eu.bitwalker.useragentutils.UserAgent;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
@Controller
@RequestMapping("/user/")
public class LoginControl extends BaseController {

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
        String token = getParameter(request, "token");
        if (StringUtils.isNotBlank(token)) {
            ID tokenUser = AuthTokenManager.verifyToken(token, true);
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
        String alt = ServletUtils.readCookie(request, CK_AUTOLOGIN);
        if (StringUtils.isNotBlank(alt)) {
            ID altUser = null;
            try {
                alt = AES.decrypt(alt);
                String[] alts = alt.split(",");
                altUser = ID.isId(alts[0]) ? ID.valueOf(alts[0]) : null;

                // 最大一个月有效期
                if (altUser != null) {
                    long t = ObjectUtils.toLong(alts[1]);
                    if ((System.currentTimeMillis() - t) / 1000 > 30 * 24 * 60 * 60) {
                        altUser = null;
                    }
                }

            } catch (Exception ex) {
                LOG.error("Cannot decode User from alt : " + alt, ex);
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
        return createModelAndView("/signup/login");
    }

    @PostMapping("user-login")
    public void userLogin(HttpServletRequest request, HttpServletResponse response) {
        String vcode = getParameter(request, "vcode");
        Boolean needVcode = (Boolean) ServletUtils.getSessionAttribute(request, SK_NEED_VCODE);
        if (needVcode != null && needVcode
                && (StringUtils.isBlank(vcode) || !CaptchaUtil.ver(vcode, request))) {
            writeFailure(response, getLang(request, "SomeError", "Captcha"));
            return;
        }

        final String user = getParameterNotNull(request, "user");
        final String password = ServletUtils.getRequestString(request);

        int retry = getLoginRetryTimes(user, 1);
        if (retry > 3 && StringUtils.isBlank(vcode)) {
            ServletUtils.setSessionAttribute(request, SK_NEED_VCODE, true);
            writeFailure(response, "VCODE");
            return;
        }

        String hasError = LoginToken.checkUser(user, password);
        if (hasError != null) {
            writeFailure(response, hasError);
            return;
        }

        User loginUser = Application.getUserStore().getUser(user);
        loginSuccessed(request, response, loginUser.getId(), getBoolParameter(request, "autoLogin", false));

        // 清理
        getLoginRetryTimes(user, -1);
        ServletUtils.setSessionAttribute(request, SK_NEED_VCODE, null);

        String danger = ServerStatus.checkValidity();
        if (danger != null) {
            writeSuccess(response, JSONUtils.toJSONObject("danger", danger));
        } else {
            writeSuccess(response);
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
            AppUtils.addCookie(response, CK_AUTOLOGIN, alt);
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
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletUtils.removeCookie(request, response, CK_AUTOLOGIN);
        ServletUtils.getSession(request).invalidate();
        response.sendRedirect("login");
    }

    // --

    @GetMapping("forgot-passwd")
    public ModelAndView forgotPasswd() {
        return createModelAndView("/signup/forgot-passwd");
    }

    @PostMapping("user-forgot-passwd")
    public void userForgotPasswd(HttpServletRequest request, HttpServletResponse response) {
        if (!SMSender.availableMail()) {
            writeFailure(response, "邮件服务账户未配置，请联系管理员配置");
            return;
        }

        String email = getParameterNotNull(request, "email");
        if (!RegexUtils.isEMail(email) || !Application.getUserStore().existsEmail(email)) {
            writeFailure(response, "邮箱无效");
            return;
        }

        String vcode = VerfiyCode.generate(email, 2);
        String content = "你的重置密码验证码是：" + vcode;
        String sentid = SMSender.sendMail(email, "重置密码", content);
        if (sentid != null) {
            writeSuccess(response);
        } else {
            writeFailure(response);
        }
    }

    @PostMapping("user-confirm-passwd")
    public void userConfirmPasswd(HttpServletRequest request, HttpServletResponse response) {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        String newpwd = data.getString("newpwd");
        String email = data.getString("email");
        String vcode = data.getString("vcode");

        if (!VerfiyCode.verfiy(email, vcode, true)) {
            writeFailure(response, getLang(request, "SomeInvalid", "Captcha"));
            return;
        }

        User user = Application.getUserStore().getUserByEmail(email);
        Record record = EntityHelper.forUpdate(user.getId(), user.getId());
        record.setString("password", newpwd);
        try {
            UserContext.setUser(user.getId());

            Application.getBean(UserService.class).update(record);
            writeSuccess(response);
            VerfiyCode.clean(email);
        } catch (DataSpecificationException ex) {
            writeFailure(response, ex.getLocalizedMessage());
        } finally {
            UserContext.clear();
        }
    }

    @GetMapping("live-wallpaper")
    public void getLiveWallpaper(HttpServletResponse response) {
        if (!RebuildConfiguration.getBool(ConfigurationItem.LiveWallpaper)) {
            writeFailure(response);
            return;
        }

        JSONObject ret = License.siteApi("api/misc/bgimg", true);
        if (ret == null) {
            writeFailure(response);
        } else {
            writeSuccess(response, ret.getString("url"));
        }
    }
}
