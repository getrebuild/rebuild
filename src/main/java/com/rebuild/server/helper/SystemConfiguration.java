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

import org.apache.commons.collections4.map.CaseInsensitiveMap;
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
public class SystemConfiguration {
	
	private static final Log LOG = LogFactory.getLog(SystemConfiguration.class);
	
	/**
	 * 临时目录/文件
	 * 
	 * @param file
	 * @return
	 */
	public static File getFileOfTemp(String file) {
		String tmp = getItem(SystemItem.TempDirectory);
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
		return getItem(SystemItem.StorageURL);
	}
	
	/**
	 * 云存储账号
	 * 
	 * @return returns [StorageApiKey, StorageApiSecret, StorageBucket, StorageURL]
	 */
	public static String[] getStorageAccount() {
		String key = getItem(SystemItem.StorageApiKey);
		if (key == null) {
			return null;
		}
		String secret = getItem(SystemItem.StorageApiSecret);
		if (secret == null) {
			return null;
		}
		String bucket = getItem(SystemItem.StorageBucket);
		if (bucket == null) {
			return null;
		}
		return new String[] { key, secret, bucket, getStorageUrl() };
	}
	
	/**
	 * 缓存账号
	 * 
	 * @return returns [CacheHost, CachePort, CacheUser, CachePassword]
	 */
	public static String[] getCacheAccount() {
		String host = getItem(SystemItem.CacheHost);
		if (host == null) {
			return null;
		}
		String port = getItem(SystemItem.CachePort);
		if (port == null) {
			return null;
		}
		String password = getItem(SystemItem.CachePassword);
		if (password == null) {
			return null;
		}
		String user = getItem(SystemItem.CacheUser);
		return new String[] { host, port, user, password };
	}
	
	/**
	 * 邮件账号
	 * 
	 * @return returns [MailUser, MailPassword, MailAddr, MailName]
	 */
	public static String[] getEmailAccount() {
		String user = getItem(SystemItem.MailUser);
		if (user == null) {
			return null;
		}
		String password = getItem(SystemItem.MailPassword);
		if (password == null) {
			return null;
		}
		String addr = getItem(SystemItem.MailAddr);
		if (addr == null) {
			return null;
		}
		String name = getItem(SystemItem.MailName);
		if (name == null) {
			return null;
		}
		return new String[] { user, password, addr, name };
	}
	
	/**
	 * 短信账号
	 * 
	 * @return returns [SmsUser, SmsPassword, SmsSign]
	 */
	public static String[] getSmsAccount() {
		String user = getItem(SystemItem.SmsUser);
		if (user == null) {
			return null;
		}
		String password = getItem(SystemItem.SmsPassword);
		if (password == null) {
			return null;
		}
		String sign = getItem(SystemItem.SmsSign);
		if (sign == null) {
			return null;
		}
		return new String[] { user, password, sign };
	}
	
	// --
	
	private static final Map<String, String> CACHED = new CaseInsensitiveMap<>();
	
	/**
	 * @param name
	 * @return
	 */
	public static String getItem(SystemItem name) {
		return getItem(name, false);
	}
	
	/**
	 * @param name
	 * @return
	 */
	public static String getItem(SystemItem name, boolean reload) {
		String s = CACHED.get(name.name());
		if (s != null && reload == false) {
			return s;
		}
		
		Object[] value = Application.createQueryNoFilter(
				"select value from SystemConfig where item = ?")
				.setParameter(1, name.name())
				.unique();
		s = value == null ? null : StringUtils.defaultIfBlank((String) value[0], null);
		
		// 从配置文件加载
		if (s == null) {
			s = Application.getBean(AesPreferencesConfigurer.class).getItem(name.name());
		}
		
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
	public static long getLongItem(SystemItem name, long defaultValue) {
		String s = getItem(name);
		return s == null ? defaultValue : NumberUtils.toLong(s);
	}
	
	/**
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static boolean getBoolItem(SystemItem name, boolean defaultValue) {
		String s = getItem(name);
		return s == null ? defaultValue : BooleanUtils.toBoolean(s);
	}
	
	/**
	 * @param name
	 * @param value
	 * @return
	 */
	public static void setItem(SystemItem name, Object value) {
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
