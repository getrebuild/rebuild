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
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.KVStorage;
import com.rebuild.core.support.License;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.web.BaseController;
import com.rebuild.web.user.UserAvatar;
import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.extern.slf4j.Slf4j;
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

    private static final String SK_SHOW_TOUR = "showStartTour";
    private static final String SK_SHOW_GUIDE = "showStartGuide";

    public static final String SK_TEMP_AUTH = "rbTempAuth";

    protected static final String PREFIX_2FA = "2FA:";
    protected static final String PREFIX_ALT = "ALT:";

    /**
     * @param request
     * @param response
     * @param user
     * @param autoLogin
     * @return
     */
    protected Integer loginSuccessed(HttpServletRequest request, HttpServletResponse response, ID user, boolean autoLogin) {
        return loginSuccessed(request, response, user, autoLogin, false);
    }

    /**
     * @param request
     * @param response
     * @param user
     * @return
     */
    protected Map<String, Object> loginSuccessedH5(HttpServletRequest request, HttpServletResponse response, ID user) {
        Map<String, Object> resMap = new HashMap<>();

        Integer ed = loginSuccessed(request, response, user, false, true);
        if (ed != null) resMap.put("passwdExpiredDays", ed);

        String authToken = AuthTokenManager.generateAccessToken(user);
        resMap.put("authToken", authToken);

        request.getSession().invalidate();
        return resMap;
    }

    /**
     * 登录成功
     *
     * @param request
     * @param response
     * @param user
     * @param autoLogin
     * @param fromH5
     * @return 密码过期时间（如有）
     */
    private Integer loginSuccessed(HttpServletRequest request, HttpServletResponse response, ID user, boolean autoLogin, boolean fromH5) {
        // 自动登录
        if (autoLogin) {
            final String altToken = CodecUtils.randomCode(60);
            Application.getCommonsCache().putx(PREFIX_ALT + altToken, user, CommonsCache.TS_DAY * 14);
            ServletUtils.addCookie(response, CK_AUTOLOGIN, altToken);
        } else {
            ServletUtils.removeCookie(request, response, CK_AUTOLOGIN);
        }

        createLoginLog(request, user);

        // TODO H5
        if (!fromH5) {
            ServletUtils.setSessionAttribute(request, WebUtils.CURRENT_USER, user);
            ServletUtils.setSessionAttribute(request, SK_USER_THEME, KVStorage.getCustomValue("THEME." + user));
            Application.getSessionStore().storeLoginSuccessed(request, fromH5);

            // 头像缓存
            ServletUtils.setSessionAttribute(request, UserAvatar.SK_DAVATAR, System.currentTimeMillis());

            // v3.2 GUIDE 显示规则
            if (UserHelper.isSuperAdmin(user)) {
                Object GuideShowNaver = KVStorage.getCustomValue("GuideShowNaver");
                if (!ObjectUtils.toBool(GuideShowNaver) || CommandArgs.getBoolean(CommandArgs._ForceTour)) {
                    ServletUtils.setSessionAttribute(request, SK_SHOW_GUIDE, Boolean.TRUE);
                    // v3.8 禁用
                    ServletUtils.setSessionAttribute(request, SK_SHOW_GUIDE, Boolean.FALSE);
                }
            }
            // TOUR 显示规则
            Object[] initLoginTimes = Application.createQueryNoFilter(
                    "select count(loginTime) from LoginLog where user = ? and loginTime > '2022-01-01'")
                    .setParameter(1, user)
                    .unique();
            if (ObjectUtils.toLong(initLoginTimes[0]) <= 10 || CommandArgs.getBoolean(CommandArgs._ForceTour)) {
                ServletUtils.setSessionAttribute(request, SK_SHOW_TOUR, Boolean.TRUE);
            }
        }

        // 密码过期剩余时间
        Integer ed = UserService.getPasswdExpiredDayLeft(user);
        return ed == null || ed > 14 ? null : ed;
    }

    /**
     * 登录日志
     *
     * @param request
     * @param user
     */
    private void createLoginLog(HttpServletRequest request, ID user) {
        final String userAgent = request.getHeader("user-agent");
        String uaSimple;
        try {
            final UserAgent UA = UserAgent.parseUserAgentString(userAgent);

            uaSimple = UA.getBrowser().name();
            if (UA.getBrowserVersion() != null) {
                String mv = UA.getBrowserVersion().getMajorVersion();
                if (!uaSimple.endsWith(mv)) uaSimple += "-" + mv;
            }

            OperatingSystem os = UA.getOperatingSystem();
            if (os != null) {
                uaSimple += " (" + os + ")";
                if (os.getDeviceType() != null && os.getDeviceType() == DeviceType.MOBILE) uaSimple += " [Mobile]";
            }

            if (request.getAttribute(SK_TEMP_AUTH) != null) uaSimple += " [TempAuth]";
            if (userAgent.contains("DingTalk")) uaSimple += " [DingTalk]";
            if (userAgent.contains("wxwork")) uaSimple += " [WeCom]";

        } catch (Exception ex) {
            log.warn("Unknown User-Agent : {}", userAgent);
            uaSimple = "UNKNOW";
        }

        String ipAddr = StringUtils.defaultString(ServletUtils.getRemoteAddr(request), "127.0.0.1");

        final Record llog = EntityHelper.forNew(EntityHelper.LoginLog, UserService.SYSTEM_USER);
        llog.setID("user", user);
        llog.setString("ipAddr", ipAddr);
        llog.setString("userAgent", uaSimple);
        llog.setDate("loginTime", CalendarUtils.now());

        TaskExecutors.queue(() -> {
            Application.getCommonsService().create(llog);

            User u = Application.getUserStore().getUser(user);
            String uid = StringUtils.defaultString(u.getEmail(), u.getName());
            if (uid == null) uid = user.toLiteral();
            
            String uaUrl = String.format("api/authority/user/echo?user=%s&ip=%s&ua=%s&source=%s",
                    CodecUtils.base64UrlEncode(uid), ipAddr, CodecUtils.urlEncode(userAgent),
                    CodecUtils.base64UrlEncode(request.getRequestURL().toString()));
            License.siteApiNoCache(uaUrl);
        });
    }
}
