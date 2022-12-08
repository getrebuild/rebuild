/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user.signup;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.KVStorage;
import com.rebuild.core.support.License;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.web.BaseController;
import com.rebuild.web.user.UserAvatar;
import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2022/3/4
 */
@Slf4j
public class LoginAction extends BaseController {

    public static final String CK_AUTOLOGIN = "rb.alt";
    public static final String SK_USER_THEME = "currentUseTheme";

    protected static final String SK_NEED_VCODE = "needLoginVCode";
    protected static final String SK_START_TOUR = "needStartTour";

    protected static final String PREFIX_2FA = "2FA:";
    protected static final String PREFIX_ALT = "ALT:";

    /**
     * 登录成功
     *
     * @param request
     * @param response
     * @param user
     * @param autoLogin
     * @return 密码过期时间（如有）
     */
    protected Integer loginSuccessed(HttpServletRequest request, HttpServletResponse response, ID user, boolean autoLogin) {
        // 自动登录
        if (autoLogin) {
            final String altToken = CodecUtils.randomCode(60);
            Application.getCommonsCache().putx(PREFIX_ALT + altToken, user, CommonsCache.TS_DAY * 14);
            ServletUtils.addCookie(response, CK_AUTOLOGIN, altToken);
        } else {
            ServletUtils.removeCookie(request, response, CK_AUTOLOGIN);
        }

        createLoginLog(request, user);

        ServletUtils.setSessionAttribute(request, WebUtils.CURRENT_USER, user);
        ServletUtils.setSessionAttribute(request, SK_USER_THEME, KVStorage.getCustomValue("THEME." + user));
        Application.getSessionStore().storeLoginSuccessed(request);

        // 头像缓存
        ServletUtils.setSessionAttribute(request, UserAvatar.SK_DAVATAR, System.currentTimeMillis());

        // v3.2 Guide
        if (UserHelper.isSuperAdmin(user)) {
            Object GuideShowNaver = KVStorage.getCustomValue("GuideShowNaver");
            if (!ObjectUtils.toBool(GuideShowNaver)) {
                ServletUtils.setSessionAttribute(request, "GuideShow", true);
            }
        }

        // TOUR 显示规则
        Object[] initLoginTimes = Application.createQueryNoFilter(
                "select count(loginTime) from LoginLog where user = ? and loginTime > '2022-01-01'")
                .setParameter(1, user)
                .unique();
        if (ObjectUtils.toLong(initLoginTimes[0]) <= 10
                || BooleanUtils.toBoolean(System.getProperty("_ForceTour"))) {
            ServletUtils.setSessionAttribute(request, SK_START_TOUR, "yes");
        }

        // 密码过期剩余时间
        Integer ed = UserService.getPasswdExpiredDayLeft(user);
        return ed == null || ed > 14 ? null : ed;
    }

    /**
     * 登录成功 H5
     *
     * @param request
     * @param response
     * @param user
     * @return
     */
    protected Map<String, Object> loginSuccessedH5(HttpServletRequest request, HttpServletResponse response, ID user) {
        Map<String, Object> resMap = new HashMap<>();

        Integer ed = loginSuccessed(request, response, user, false);
        if (ed != null) resMap.put("passwdExpiredDays", ed);

        String authToken = AuthTokenManager.generateAccessToken(user);
        resMap.put("authToken", authToken);

        request.getSession().invalidate();
        return resMap;
    }

    /**
     * @param request
     * @param user
     */
    private void createLoginLog(HttpServletRequest request, ID user) {
        final String ua = request.getHeader("user-agent");
        String uaClear;
        try {
            UserAgent uas = UserAgent.parseUserAgentString(ua);

            uaClear = uas.getBrowser().name();
            if (uas.getBrowserVersion() != null) {
                String mv = uas.getBrowserVersion().getMajorVersion();
                if (!uaClear.endsWith(mv)) uaClear += "-" + mv;
            }

            OperatingSystem os = uas.getOperatingSystem();
            if (os != null) {
                uaClear += " (" + os + ")";
                if (os.getDeviceType() != null && os.getDeviceType() == DeviceType.MOBILE) uaClear += " [Mobile]";
            }

        } catch (Exception ex) {
            log.warn("Unknown user-agent : {}", ua);
            uaClear = "UNKNOW";
        }

        String ipAddr = StringUtils.defaultString(ServletUtils.getRemoteAddr(request), "127.0.0.1");

        final Record llog = EntityHelper.forNew(EntityHelper.LoginLog, UserService.SYSTEM_USER);
        llog.setID("user", user);
        llog.setString("ipAddr", ipAddr);
        llog.setString("userAgent", uaClear);
        llog.setDate("loginTime", CalendarUtils.now());

        TaskExecutors.queue(() -> {
            Application.getCommonsService().create(llog);

            User u = Application.getUserStore().getUser(user);
            String uid = StringUtils.defaultString(u.getEmail(), u.getName());
            if (uid == null) uid = user.toLiteral();
            
            String uaUrl = String.format("api/authority/user/echo?user=%s&ip=%s&ua=%s",
                    CodecUtils.base64UrlEncode(uid), ipAddr, CodecUtils.urlEncode(ua));
            License.siteApiNoCache(uaUrl);
        });
    }
}
