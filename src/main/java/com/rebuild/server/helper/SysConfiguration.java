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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 全局系统配置
 * 
 * @author devezhao
 * @since 10/14/2018
 * @see ConfigurableItem
 */
public class SysConfiguration {
	
	private static final Log LOG = LogFactory.getLog(SysConfiguration.class);
	
	/**
	 * 获取数据目录下的文件（或目录）
	 * 
	 * @param file
	 * @return
	 */
	public static File getFileOfData(String file) {
		String d = get(ConfigurableItem.DataDirectory, null);
		File dFile = null;
		if (d != null) {
			dFile = new File(d);
			if (!dFile.exists()) {
				if (!dFile.mkdirs()) {
					LOG.warn("TempDirectory not exists : " + d);
					dFile = FileUtils.getTempDirectory();
				}
			}
		} else {
			dFile = FileUtils.getTempDirectory();
		}
		return new File(dFile, file);
	}
	
	/**
	 * 获取临时文件（或目录）
	 * 
	 * @param file
	 * @return
	 * @see #getFileOfData(String)
	 */
	public static File getFileOfTemp(String file) {
		File tFile = getFileOfData("tmp");
		if (!tFile.exists()) {
			tFile.mkdirs();
		}
		return new File(tFile, file);
	}
	
	/**
	 * 获取配置文件 
	 * 
	 * @param file
	 * @return
	 */
	public static File getFileOfRes(String file) {
		URL fileUrl = SysConfiguration.class.getClassLoader().getResource(file);
		try {
			File resFile = new File(fileUrl.toURI());
			return resFile;
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Bad file path or name : " + file);
		}
	}
	
	/**
	 * 云存储地址
	 * 
	 * @return
	 */
	public static String getStorageUrl() {
		String account[] = getStorageAccount();
		return account == null ? null : account[3];
	}
	
	/**
	 * 云存储账号
	 * 
	 * @return returns [StorageApiKey, StorageApiSecret, StorageBucket, StorageURL]
	 */
	public static String[] getStorageAccount() {
		return getsNoUnset(
				ConfigurableItem.StorageApiKey, ConfigurableItem.StorageApiSecret, ConfigurableItem.StorageBucket, ConfigurableItem.StorageURL);
	}
	
	/**
	 * 缓存账号
	 * 
	 * @return returns [CacheHost, CachePort, CachePassword]
	 */
	public static String[] getCacheAccount() {
		return getsNoUnset(
				ConfigurableItem.CacheHost, ConfigurableItem.CachePort, ConfigurableItem.CachePassword);
	}
	
	/**
	 * 邮件账号
	 * 
	 * @return returns [MailUser, MailPassword, MailAddr, MailName]
	 */
	public static String[] getMailAccount() {
		return getsNoUnset(
				ConfigurableItem.MailUser, ConfigurableItem.MailPassword, ConfigurableItem.MailAddr, ConfigurableItem.MailName);
	}
	
	/**
	 * 短信账号
	 * 
	 * @return returns [SmsUser, SmsPassword, SmsSign]
	 */
	public static String[] getSmsAccount() {
		return getsNoUnset(
				ConfigurableItem.SmsUser, ConfigurableItem.SmsPassword, ConfigurableItem.SmsSign);
	}
	
	/**
	 * 获取多个，任意一个为空都返回 null
	 * 
	 * @param items
	 * @return
	 */
	private static String[] getsNoUnset(ConfigurableItem... items) {
		List<String> list = new ArrayList<>();
		for (ConfigurableItem item : items) {
			String v = get(item, false);
			if (v == null) {
				return null;
			}
			list.add(v);
		}
		return list.toArray(new String[list.size()]);
	}
	
	// --
	
	/**
	 * @param name
	 * @param reload
	 * @return
	 */
	public static String get(ConfigurableItem name, boolean reload) {
		final String key = name.name();
		String s = Application.getCommonCache().get(key);
		if (s != null && !reload) {
			return s;
		}
		
		// 1. 首先从数据库
		Object[] fromDb = Application.createQueryNoFilter(
				"select value from SystemConfig where item = ?")
				.setParameter(1, name.name())
				.unique();
		s = fromDb == null ? null : StringUtils.defaultIfBlank((String) fromDb[0], null);
		
		// 2. 从配置文件加载
		if (s == null) {
			s = Application.getBean(AesPreferencesConfigurer.class).getItem(key);
		}
		
		// 3. 默认值
		if (s == null && name.getDefaultValue() != null) {
			s = name.getDefaultValue().toString();
		}
		
		if (s == null) {
			Application.getCommonCache().evict(key);
		} else {
			Application.getCommonCache().put(key, s, 2 * 60 * 60);
		}
		return s;
	}
	
	/**
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static String get(ConfigurableItem name, String defaultValue) {
		String s = get(name, false);
		if (s == null) {
			Object v = defaultValue != null ? defaultValue : name.getDefaultValue();
			return v == null ? null : v.toString();
		}
		return s;
	}
	
	/**
	 * @param name
	 * @return
	 */
	public static long getLong(ConfigurableItem name) {
		String s = get(name, false);
		return s == null ? (Long) name.getDefaultValue() : NumberUtils.toLong(s);
	}
	
	/**
	 * @param name
	 * @return
	 */
	public static boolean getBool(ConfigurableItem name) {
		String s = get(name, false);
		return s == null ? (Boolean) name.getDefaultValue() : BooleanUtils.toBoolean(s);
	}
	
	/**
	 * @param name
	 * @param value
	 * @return
	 */
	public static void set(ConfigurableItem name, Object value) {
		Object[] exists = Application.createQueryNoFilter(
				"select configId from SystemConfig where item = ?")
				.setParameter(1, name.name())
				.unique();
		
		Record record = null;
		if (exists == null) {
			record = EntityHelper.forNew(EntityHelper.SystemConfig, UserService.SYSTEM_USER);
			record.setString("item", name.name());
		} else {
			record = EntityHelper.forUpdate((ID) exists[0], UserService.SYSTEM_USER);
		}
		record.setString("value", value.toString());
		
		Application.getCommonService().createOrUpdate(record);
		get(name, true);
	}
}
