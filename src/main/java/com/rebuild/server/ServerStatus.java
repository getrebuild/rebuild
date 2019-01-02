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

package com.rebuild.server;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.helper.SystemConfiguration;
import com.rebuild.server.helper.cache.BaseCacheTemplate;
import com.rebuild.server.helper.cache.CacheTemplate;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ThrowableUtils;

/**
 * 各服务状态
 * 
 * @author devezhao
 * @since 10/31/2018
 */
public class ServerStatus {

	private static final List<State> LAST_STATUS = new ArrayList<>();
	
	/**
	 * 最近状态
	 * 
	 * @return
	 */
	public static List<State> getLastStatus() {
		synchronized (LAST_STATUS) {
			return Collections.unmodifiableList(LAST_STATUS);
		}
	}
	/**
	 * 服务是否正常
	 * 
	 * @return
	 */
	public static boolean isStatusOK() {
		for (State s : getLastStatus()) {
			if (!s.success) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 启动检查
	 * 
	 * @return
	 */
	public static boolean checkAll() {
		List<State> last = new ArrayList<>();
		
		State stateCreateFile = checkCreateFile();
		Application.LOG.info("Checking " + stateCreateFile);
		last.add(stateCreateFile);
		
		State stateDatabase = checkDatabase();
		Application.LOG.info("Checking " + stateDatabase);
		last.add(stateDatabase);
		
		State stateCacheService = checkCacheService();
		Application.LOG.info("Checking " + stateCacheService);
		last.add(stateCacheService);
		
		synchronized (LAST_STATUS) {
			LAST_STATUS.clear();
			LAST_STATUS.addAll(last);
		}
		return isStatusOK();
	}

	/**
	 * 数据库连接
	 * 
	 * @return
	 */
	protected static State checkDatabase() {
		String name = "Database";
		try {
			DataSource ds = Application.getPersistManagerFactory().getDataSource();
			Connection c = DataSourceUtils.getConnection(ds);
			DataSourceUtils.releaseConnection(c, ds);
		} catch (Exception ex) {
			return State.error(name, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
		}
		return State.success(name);
	}
	
	/**
	 * 文件权限/磁盘空间
	 * 
	 * @return
	 */
	protected static State checkCreateFile() {
		String name = "CreateFile";
		FileWriter fw = null;
		try {
			File test = SystemConfiguration.getFileOfTemp("test");
			fw = new FileWriter(test);
			IOUtils.write(CodecUtils.randomCode(100), fw);
			if (!test.exists()) {
				return State.error(name, "Cloud't create file in temp Directory");
			} else {
				test.delete();
			}
			
		} catch (Exception ex) {
			return State.error(name, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
		} finally {
			IOUtils.closeQuietly(fw);
		}
		return State.success(name);
	}
	
	/**
	 * 缓存系统
	 * 
	 * @return
	 */
	protected static State checkCacheService() {
		String name = "Cache/";
		CacheTemplate cacheTemplate = Application.getCommonCache();
		String type = ((BaseCacheTemplate) cacheTemplate).isUseRedis() ? "REDIS" : "EHCACHE";
		name += type;
		
		try {
			cacheTemplate.putx("test", 1, 60);
		} catch (Exception ex) {
			return State.error(name, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
		}
		return State.success(name);
	}
	
	// 状态
	public static class State {
		final public String name;
		final public boolean success;
		final public String error;
		@Override
		public String toString() {
			if (success) return String.format("%s : %s", name, "[ SUCCESS ]");
			else return String.format("%s : %s", name, error);
		}
		public JSON toJson() {
			return JSONUtils.toJSONObject(name, success ? true : error);
		}
		
		private State(String name, boolean success, String error) {
			this.name = name;
			this.success = success;
			this.error = error;
		}
		private static State success(String name) {
			return new State(name, true, null);
		}
		private static State error(String name, String error) {
			return new State(name, false, error);
		}
	}
}
