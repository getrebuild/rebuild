/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
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
	LOGO, LOGOWhite,
	HomeURL("https://nightly.getrebuild.com/"),
	OpenSignUp(false),

	// 数据目录
	DataDirectory,
	
	// 云存储
	StorageURL, StorageApiKey, StorageApiSecret, StorageBucket,
	
	// 缓存服务
	CacheHost, CachePort, CacheUser, CachePassword,
	
	// 邮件
	MailUser, MailPassword, MailAddr, MailName(AppName),
	
	// 短信
	SmsUser, SmsPassword, SmsSign(AppName),
	
	// Database schema version
	DBVer(0L),
	
	// 启用最近搜素缓存
	EnableRecentlyUsed(true),

	// 登录背景图
	LiveWallpaper(true),

	// 回收站数据保留天数（0为禁用回收站）
	RecycleBinKeepingDays(30),

	// 密码策略，1-3
	PasswordPolicy(1),

	;
	
	private Object defaultVal;
	
	ConfigurableItem() {
	}

	ConfigurableItem(Object defaultVal) {
		this.defaultVal = defaultVal;
	}
	
	/**
	 * 默认值
	 * s
	 * @return
	 */
	public Object getDefaultValue() {
		if (defaultVal != null && defaultVal instanceof ConfigurableItem) {
			return ((ConfigurableItem) defaultVal).getDefaultValue();
		}
		return defaultVal;
	}
}
