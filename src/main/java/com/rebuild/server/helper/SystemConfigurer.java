/*
rebuild - Building your system freely.
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;
import com.rebuild.server.bizz.UserService;
import com.rebuild.server.metadata.EntityHelper;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 系统配置
 * 
 * @author devezhao
 * @since 10/14/2018
 */
public class SystemConfigurer {
	
	/**
	 * 系统配置项名称
	 */
	public static enum ItemName {
		// 临时目录
		TempDirectory,
		// 云存储
		StorageURL, StorageApiKey, StorageApiSecret, StorageBucket,
		// 缓存服务
		CacheHost, CachePort, CacheUser, CachePassword,
	}
	
	private static final Log LOG = LogFactory.getLog(SystemConfigurer.class);
	
	/**
	 * 临时目录/文件
	 * 
	 * @param file
	 * @return
	 */
	public static File getFileOfTemp(String file) {
		String tmp = getItemFromBoth(ItemName.TempDirectory);
		File tmp2 = null;
		if (tmp != null) {
			tmp2 = new File(tmp);
			if (!tmp2.exists()) {
				LOG.warn("TempDirectory not exists : " + tmp);
				tmp2 = FileUtils.getTempDirectory();
			}
		} else {
			tmp2 = FileUtils.getTempDirectory();
		}
		return new File(tmp2, file);
	}
	
	/**
	 * 云存储地址
	 * 
	 * @return
	 */
	public static String getStorageUrl() {
		return getItemFromBoth(ItemName.StorageURL);
	}
	
	/**
	 * 云存储账号
	 * 
	 * @return
	 */
	public static String[] getStorageAccount() {
		String key = getItemFromBoth(ItemName.StorageApiKey);
		if (key == null) {
			return null;
		}
		String secret = getItemFromBoth(ItemName.StorageApiSecret);
		if (secret == null) {
			return null;
		}
		String bucket = getItemFromBoth(ItemName.StorageBucket);
		if (bucket == null) {
			return null;
		}
		return new String[] { key, secret, bucket };
	}
	
	public static String[] getCacheAccount() {
		String host = getItemFromBoth(ItemName.CacheHost);
		if (host == null) {
			return null;
		}
		String port = getItemFromBoth(ItemName.CachePort);
		if (port == null) {
			return null;
		}
		String user = getItemFromBoth(ItemName.CacheUser);
//		if (user == null) {
//			return null;
//		}
		String password = getItemFromBoth(ItemName.CachePassword);
		if (password == null) {
			return null;
		}
		return new String[] { host, port, user, password };
	}
	
	/*-
	 * 从数据库-配置文件获取
	 */
	private static String getItemFromBoth(ItemName name) {
		String s = getItem(name);
		if (s == null) {
			s = Application.getBean(AesPreferencesConfigurer.class).getItem(name.name());
		}
		return s;
	}
	
	// --
	
	private static final Map<String, String> CACHED = new ConcurrentHashMap<>();
	
	/**
	 * @param name
	 * @return
	 */
	public static String getItem(ItemName name) {
		return getItem(name, false);
	}
	
	/**
	 * @param name
	 * @return
	 */
	public static String getItem(ItemName name, boolean reload) {
		String s = CACHED.get(name.name());
		if (s != null && reload == false) {
			return s;
		}
		
		Object[] value = Application.createQueryNoFilter(
				"select value from SystemConfig where item = ?")
				.setParameter(1, name.name())
				.unique();
		s = value == null ? null : StringUtils.defaultIfBlank((String) value[0], null);
		if (s == null) {
			CACHED.remove(name.name());
		} else {
			CACHED.put(name.name(), s);
		}
		return s;
	}
	
	/**
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static long getLongItem(ItemName name, long defaultValue) {
		String s = getItem(name);
		return s == null ? defaultValue : NumberUtils.toLong(s);
	}
	
	/**
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static boolean getBoolItem(ItemName name, boolean defaultValue) {
		String s = getItem(name);
		return s == null ? defaultValue : BooleanUtils.toBoolean(s);
	}
	
	/**
	 * @param name
	 * @param value
	 * @return
	 */
	public static void setItem(ItemName name, Object value) {
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
		getItem(name, true);
	}
}
