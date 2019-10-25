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

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import com.rebuild.server.helper.cache.CommonCache;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

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
		String d = get(ConfigurableItem.DataDirectory);
		File dir = null;
		if (d != null) {
			dir = new File(d);
			if (!dir.exists()) {
			    if (!dir.mkdirs()) {
			        LOG.error("Couldn't mkdirs for data : " + dir);
                }
			}
		}

		if (dir == null || !dir.exists()) {
			dir = FileUtils.getUserDirectory();
			dir = new File(dir, ".rebuild");
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
                    LOG.error("Couldn't mkdirs for data : " + dir);
                }
			}
		}

		if (!dir.exists()) {
			dir = FileUtils.getTempDirectory();
		}

		return new File(dir, file);
	}
	
	/**
	 * 获取临时文件（或目录）
	 * 
	 * @param file
	 * @return
	 * @see #getFileOfData(String)
	 */
	public static File getFileOfTemp(String file) {
		File tFile = getFileOfData("temp");
		if (!tFile.exists()) {
			if (!tFile.mkdirs()) {
				throw new RebuildException("Couldn't mkdirs : " + tFile);
			}
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
		try {
			return ResourceUtils.getFile("classpath:" + file);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Bad file path or name : " + file);
		}
	}

	/**
	 * 云存储地址
	 * 
	 * @return
	 */
	public static String getStorageUrl() {
		String[] account = getStorageAccount();
		return account == null ? null : account[3];
	}
	
	/**
	 * 云存储账号
	 * 
	 * @return returns [StorageApiKey, StorageApiSecret, StorageBucket, StorageURL]
	 */
	public static String[] getStorageAccount() {
		return getsNoUnset(false,
				ConfigurableItem.StorageApiKey, ConfigurableItem.StorageApiSecret, ConfigurableItem.StorageBucket, ConfigurableItem.StorageURL);
	}
	
	/**
	 * 缓存账号
	 * 
	 * @return returns [CacheHost, CachePort, CachePassword]
	 */
	public static String[] getCacheAccount() {
		return getsNoUnset(false,
				ConfigurableItem.CacheHost, ConfigurableItem.CachePort, ConfigurableItem.CachePassword);
	}
	
	/**
	 * 邮件账号
	 * 
	 * @return returns [MailUser, MailPassword, MailAddr, MailName]
	 */
	public static String[] getMailAccount() {
		return getsNoUnset(true,
				ConfigurableItem.MailUser, ConfigurableItem.MailPassword, ConfigurableItem.MailAddr, ConfigurableItem.MailName);
	}
	
	/**
	 * 短信账号
	 * 
	 * @return returns [SmsUser, SmsPassword, SmsSign]
	 */
	public static String[] getSmsAccount() {
		return getsNoUnset(true,
				ConfigurableItem.SmsUser, ConfigurableItem.SmsPassword, ConfigurableItem.SmsSign);
	}

	/**
	 * 获取首页 URL
	 *
	 * @param path 可带有路径，会自动拼接
	 * @return
	 */
	public static String getHomeUrl(String...path) {
		String homeUrl = get(ConfigurableItem.HomeURL);
		if (!homeUrl.endsWith("/")) {
			homeUrl += "/";
		}

		if (path.length > 0) {
			if (path[0].startsWith("/")) path[0] = path[0].substring(1);
			return homeUrl + path[0];
		}
		return homeUrl;
	}

	/**
	 * 获取多个，任意一个为空都返回 null
	 *
	 * @param useCache
	 * @param items
	 * @return
	 */
	private static String[] getsNoUnset(boolean useCache, ConfigurableItem... items) {
		List<String> list = new ArrayList<>();
		for (ConfigurableItem item : items) {
			String v = get(item, !useCache);
			if (v == null) {
				return null;
			}
			list.add(v);
		}
		return list.toArray(new String[0]);
	}

	// --

	/**
	 * @param name
	 * @return
	 */
	public static String get(ConfigurableItem name) {
		return get(name, false);
	}

	/**
	 * @param name
	 * @param reload
	 * @return
	 */
	public static String get(ConfigurableItem name, boolean reload) {
		return getValue(name.name(), reload, name.getDefaultValue());
	}

	/**
	 * @param name
	 * @return
	 */
	public static long getLong(ConfigurableItem name) {
		String s = get(name);
		return s == null ? (Long) name.getDefaultValue() : NumberUtils.toLong(s);
	}
	
	/**
	 * @param name
	 * @return
	 */
	public static boolean getBool(ConfigurableItem name) {
		String s = get(name);
		return s == null ? (Boolean) name.getDefaultValue() : BooleanUtils.toBoolean(s);
	}

	/**
	 * @param name
	 * @param value
	 * @return
	 */
	public static void set(ConfigurableItem name, Object value) {
		setValue(name.name(), value);
	}

	/**
	 * @param key 会自动加 `custom.` 前缀
	 * @return
	 */
	public static String getCustomValue(String key) {
		return getValue("custom." + key, false, null);
	}

	/**
	 * @param key 会自动加 `custom.` 前缀
	 * @param value
	 */
	public static void setCustomValue(String key, Object value) {
		setValue("custom." + key, value);
	}
	
	/**
	 * @param key
	 * @param value
	 */
	private static void setValue(final String key, Object value) {
		Object[] exists = Application.createQueryNoFilter(
				"select configId from SystemConfig where item = ?")
				.setParameter(1, key)
				.unique();

		Record record;
		if (exists == null) {
			record = EntityHelper.forNew(EntityHelper.SystemConfig, UserService.SYSTEM_USER);
			record.setString("item", key);
		} else {
			record = EntityHelper.forUpdate((ID) exists[0], UserService.SYSTEM_USER);
		}
		record.setString("value", value.toString());

		Application.getCommonService().createOrUpdate(record);
		Application.getCommonCache().evict(key);
	}

	/**
	 * @param key
	 * @param reload
	 * @param defaultValue
	 * @return
	 */
	private static String getValue(final String key, boolean reload, Object defaultValue) {
		if (!Application.serversReady()) {
			return defaultValue == null ? null : defaultValue.toString();
		}

		String s = Application.getCommonCache().get(key);
		if (s != null && !reload) {
			return s;
		}

		// 1. 首先从数据库
		Object[] fromDb = Application.createQueryNoFilter(
				"select value from SystemConfig where item = ?")
				.setParameter(1, key)
				.unique();
		s = fromDb == null ? null : StringUtils.defaultIfBlank((String) fromDb[0], null);

		// 2. 从配置文件加载
		if (s == null) {
			s = Application.getBean(AesPreferencesConfigurer.class).getItem(key);
		}

		// 3. 默认值
		if (s == null && defaultValue != null) {
			s = defaultValue.toString();
		}

		if (s == null) {
			Application.getCommonCache().evict(key);
		} else {
			Application.getCommonCache().put(key, s, CommonCache.TS_DAY);
		}
		return s;
	}
}
