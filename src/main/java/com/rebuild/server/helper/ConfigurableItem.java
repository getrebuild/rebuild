/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper;

/**
 * 可配置系统项，所有配置应在此处声明
 * 
 * @author devezhao
 * @since 12/25/2018
 */
public enum ConfigurableItem {

	SN,

	// 通用
	AppName("REBUILD"),
	LOGO,
    LOGOWhite,
	HomeURL("https://nightly.getrebuild.com/"),

	// 云存储
	StorageURL, StorageApiKey, StorageApiSecret, StorageBucket,

	// 缓存服务
	CacheHost, CachePort, CacheUser, CachePassword,

	// 邮件
	MailUser, MailPassword, MailAddr, MailName(AppName),

	// 短信
	SmsUser, SmsPassword, SmsSign(AppName),

	// 数据目录
	DataDirectory,
	// 数据库版本
	DBVer(0L),
	// 默认语言
	DefaultLanguage("zh-CN"),

	// 启用最近搜素
	EnableRecentlyUsed(true),

	// 开放注册
	OpenSignUp(true),
	// 登录背景图
	LiveWallpaper(true),
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
	DBBackupsEnable(false),
    // 数据备份保留时间
	DBBackupsKeepingDays(180),

	// 管理员警告
	AdminDangers(true),

	// 允许同一用户多个会话
	MultipleSessions(true),

	;
	
	private Object defaultVal;
	
	ConfigurableItem() {
	}

	ConfigurableItem(Object defaultVal) {
		this.defaultVal = defaultVal;
	}
	
	/**
	 * 默认值
	 *
	 * @return
	 */
	public Object getDefaultValue() {
		if (defaultVal != null && defaultVal instanceof ConfigurableItem) {
			return ((ConfigurableItem) defaultVal).getDefaultValue();
		}
		return defaultVal;
	}
}
