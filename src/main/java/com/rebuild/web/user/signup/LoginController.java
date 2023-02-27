/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user.signup;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.api.user.LoginToken;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CacheTemplate;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.SysbaseHeartbeat;
import com.rebuild.utils.AppUtils;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.utils.CaptchaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
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
            ID tokenUser = AuthTokenManager.verifyToken(useToken, true, false);
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
            final ID altUser = (ID) Application.getCommonsCache().getx(PREFIX_ALT + useAlt);

            if (altUser != null && UserHelper.isActive(altUser)) {
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

        mv.getModelMap().put("UsersMsg", SysbaseHeartbeat.getUsersDanger());
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
            } else if (!captchaVerify(vcode, request)) {
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

        final User loginUser = Application.getUserStore().getUser(user);
        final boolean isRbMobile = AppUtils.isRbMobile(request);

        Map<String, Object> resMap = new HashMap<>();

        // 2FA
        int faMode = RebuildConfiguration.getInt(ConfigurationItem.Login2FAMode);
        boolean faModeSkip = UserHelper.isSuperAdmin(loginUser.getId()) && !RebuildConfiguration.getBool(ConfigurationItem.SecurityEnhanced);
        if (faMode > 0 && !faModeSkip) {
            resMap.put("login2FaMode", faMode);

            final String userToken = CodecUtils.randomCode(40);
            Application.getCommonsCache().putx(PREFIX_2FA + userToken, loginUser.getId(), CommonsCache.TS_MINTE * 15);
            resMap.put("login2FaUserToken", userToken);

            if (isRbMobile) {
                request.getSession().invalidate();
            }

            return RespBody.ok(resMap);
        }

        if (isRbMobile) {
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

    @GetMapping("captcha")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Font font = new Font(Font.SERIF, Font.BOLD & Font.ITALIC, 22 + RandomUtils.nextInt(8));
        int codeLen = 4 + RandomUtils.nextInt(3);
        SpecCaptcha captcha = new SpecCaptcha(160, 41, codeLen, font);
        CaptchaUtil.out(captcha, request, response);

        // 兼容跨域
        String mobKey = request.getParameter("k");
        if (StringUtils.isNotBlank(mobKey)) {
            Application.getCommonsCache().put("MobKey" + mobKey, captcha.text(), CacheTemplate.TS_HOUR / 60);
        }
    }

    /**
     * Captcha 验证
     *
     * @param vcode
     * @param request
     * @return
     * @see #captcha(HttpServletRequest, HttpServletResponse)
     */
    public static boolean captchaVerify(String vcode, HttpServletRequest request) {
        String code = vcode.contains("/") ? vcode.split("/")[1] : vcode;
        boolean v = CaptchaUtil.ver(code, request);

        // 兼容跨域
        if (!v && vcode.contains("/") && AppUtils.isRbMobile(request)) {
            String mobKey = vcode.split("/")[0];
            String code2 = Application.getCommonsCache().get("MobKey" + mobKey);
            return code.equalsIgnoreCase(code2);
        }
        return v;
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
