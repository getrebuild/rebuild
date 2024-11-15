/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user.signup;

import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录渠道/类型
 *
 * @author devezhao
 * @since 2024/11/15
 */
@Slf4j
public enum LoginChannel {

    PC_WEB("PC浏览器"),
    PC_DINGTALK("PC钉钉"),
    PC_WECOM("PC企业微信"),
    PC_WECHAT("PC微信"),
    PC_DESKTOP("PC桌面"),

    MOB_WEB("手机浏览器"),
    MOB_DINGTALK("手机钉钉"),
    MOB_WECOM("手机企业微信"),
    MOB_WECHAT("手机微信"),
    MOB_ANDROID("手机APP"),
    MOB_IOS("iOS APP"),  // 保留

    ;

    @Getter
    private final String name;
    LoginChannel(String name) {
        this.name = name;
    }

    /**
     * @param userAgent
     * @return
     */
    public static LoginChannel parse(String userAgent) {
        userAgent = userAgent.toUpperCase();
        UserAgent UA = UserAgent.parseUserAgentString(userAgent);
        OperatingSystem OS = UA.getOperatingSystem();

        boolean isDingtalk = userAgent.contains("DINGTALK");
        boolean isWecom = userAgent.contains("WXWORK");
        boolean isWechat = !isWecom && userAgent.contains("MICROMESSENGER");
        boolean isH5PlusApp = userAgent.contains("HTML5PLUS");

        if (OS != null && OS.getDeviceType() == DeviceType.MOBILE) {
            if (isDingtalk) return MOB_DINGTALK;
            else if (isWecom) return MOB_WECOM;
            else if (isWechat) return MOB_WECHAT;
            else if (isH5PlusApp) return MOB_ANDROID;
            return MOB_WEB;
        }

        boolean isDesktop = userAgent.contains("ELECTRON");

        if (isDingtalk) return PC_DINGTALK;
        else if (isWecom) return PC_WECOM;
        else if (isWechat) return PC_WECHAT;
        else if (isDesktop) return PC_DESKTOP;
        return PC_WEB;
    }
}
