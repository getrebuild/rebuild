/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import com.rebuild.core.support.i18n.LanguageBundle;

/**
 * 系统配置项，所有配置应在此处声明
 *
 * @author devezhao
 * @since 12/25/2018
 */
public enum ConfigurationItem {

    // 系统适用
    SN, DBVer, AppBuild,

    // 缓存服务（安装/配置文件指定）
    CacheHost, CachePort, CacheUser, CachePassword,

    // 通用
    AppName("REBUILD"),
    LOGO,
    LOGOWhite,
    HomeURL("https://getrebuild.com/"),
    PageFooter,

    // 云存储
    StorageURL, StorageApiKey, StorageApiSecret, StorageBucket,

    // 邮件
    MailUser, MailPassword, MailAddr, MailName(AppName), MailCc,
    MailSmtpServer,

    // 短信
    SmsUser, SmsPassword, SmsSign(AppName),

    // 开放注册
    OpenSignUp(true),

    // 动态登录背景图
    LiveWallpaper(true),
    // 自定登录背景图
    CustomWallpaper,

    // 启用文件分享
    FileSharable(true),

    // 启用页面水印
    MarkWatermark(false),

    // 密码策略，1-3
    PasswordPolicy(1),

    // 变更历史数据保留天数（0为禁用）
    RevisionHistoryKeepingDays(180),

    // 回收站数据保留天数（0为禁用）
    RecycleBinKeepingDays(180),

    // 启用数据库备份
    DBBackupsEnable(true),

    // 数据备份保留时间（0为禁用）
    DBBackupsKeepingDays(180),

    // 允许同一用户多个会话
    MultipleSessions(true),

    // 默认语言
    DefaultLanguage(LanguageBundle.SYS_LC),

    // 视图页显示修改历史
    ShowViewHistory(true),

    // 登录验证码显示策略（1为自动，2为总是）
    LoginCaptchaPolicy(1),

    // 登录密码过期时间（0为不过期）
    PasswordExpiredDays(0),

    // 允许使用时间
    AllowUsesTime,
    // 允许使用 IP
    AllowUsesIp,
    // 2FA
    Login2FAMode(0),

    // DingTalk
    DingtalkAgentid, DingtalkAppkey, DingtalkAppsecret, DingtalkCorpid,
    DingtalkPushAeskey, DingtalkPushToken,
    DingtalkSyncUsers(false),
    DingtalkSyncUsersRole,
    // WxWork
    WxworkCorpid, WxworkAgentid, WxworkSecret,
    WxworkRxToken, WxworkRxEncodingAESKey,
    WxworkAuthFile,
    WxworkSyncUsers(false),
    WxworkSyncUsersRole,

    // PORTALs
    PortalBaiduMapAk,
    PortalOfficePreviewUrl,
    PortalUploadMaxSize(200),

    // !!! 命令行适用
    DataDirectory,                  // 数据目录
    RedisDatabase(0),     // Redis DB
    MobileUrl,                      // 移动端地址
    RbStoreUrl,                     // 在线仓库地址
    TriggerMaxDepth,                // 触发器最大执行深度
    SecurityEnhanced(false), // 安全增强
    TrustedAllUrl(false), // 可信外部地址
    LibreofficeBin,                 // Libreoffice 命令

    ;

    /**
     * 仅可通过启动参数配置
     *
     * @param name
     * @return
     */
    public static boolean inJvmArgs(String name) {
        return DataDirectory.name().equalsIgnoreCase(name)
                || RedisDatabase.name().equalsIgnoreCase(name)
                || MobileUrl.name().equalsIgnoreCase(name)
                || RbStoreUrl.name().equalsIgnoreCase(name)
                || TriggerMaxDepth.name().equalsIgnoreCase(name)
                || SecurityEnhanced.name().equalsIgnoreCase(name)
                || TrustedAllUrl.name().equalsIgnoreCase(name)
                || LibreofficeBin.name().equalsIgnoreCase(name)
                || SN.name().equalsIgnoreCase(name);
    }

    private Object defaultVal;

    ConfigurationItem() {
    }

    ConfigurationItem(Object defaultVal) {
        this.defaultVal = defaultVal;
    }

    /**
     * 默认值
     *
     * @return
     */
    public Object getDefaultValue() {
        if (defaultVal != null && defaultVal instanceof ConfigurationItem) {
            return ((ConfigurationItem) defaultVal).getDefaultValue();
        }
        return defaultVal;
    }
}
