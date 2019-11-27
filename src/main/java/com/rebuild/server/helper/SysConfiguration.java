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

import com.rebuild.server.RebuildException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;
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
public final class SysConfiguration extends KVStorage {

	/**
	 * 获取数据目录下的文件（或目录）
	 * 
	 * @param file
	 * @return
	 */
	public static File getFileOfData(String file) {
		String d = get(ConfigurableItem.DataDirectory);
		File data = null;
		if (d != null) {
			data = new File(d);
			if (!data.exists()) {
			    if (!data.mkdirs()) {
			        LOG.error("Couldn't mkdirs for data : " + data);
                }
			}
		}

		if (data == null || !data.exists()) {
			data = FileUtils.getUserDirectory();
			data = new File(data, ".rebuild");
			if (!data.exists()) {
				if (!data.mkdirs()) {
                    LOG.error("Couldn't mkdirs for data : " + data);
                }
			}
		}

		if (!data.exists()) {
			data = FileUtils.getTempDirectory();
		}
		return file == null ? data : new File(data, file);
	}
	
	/**
	 * 获取临时文件（或目录）
	 * 
	 * @param file
	 * @return
	 * @see #getFileOfData(String)
	 */
	public static File getFileOfTemp(String file) {
		File temp = getFileOfData("temp");
		if (!temp.exists()) {
			if (!temp.mkdirs()) {
				throw new RebuildException("Couldn't mkdirs : " + temp);
			}
		}
		return file == null ? temp : new File(temp, file);
	}
	
	/**
	 * 获取 classpath 下的配置文件
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
	 * 获取绝对 URL
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
	public static int getInt(ConfigurableItem name) {
		String s = get(name);
		return s == null ? (Integer) name.getDefaultValue() : NumberUtils.toInt(s);
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
}
