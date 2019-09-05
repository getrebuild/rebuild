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

package com.rebuild.server;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.SystemUtils;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.runtime.MemoryInformation;
import cn.devezhao.commons.runtime.MemoryInformationBean;
import cn.devezhao.persist4j.util.SqlHelper;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.helper.AesPreferencesConfigurer;
import com.rebuild.server.helper.cache.CommonCache;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 服务状态检查/监控
 * 
 * @author devezhao
 * @since 10/31/2018
 */
public final class ServerStatus {

	private static final List<Status> LAST_STATUS = new ArrayList<>();
	
	/**
	 * 最近状态
	 * 
	 * @return
	 */
	public static List<Status> getLastStatus() {
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
		for (Status s : getLastStatus()) {
			if (!s.success) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 系统状态检查
	 * 
	 * @return
	 */
	public static boolean checkAll() {
		List<Status> last = new ArrayList<>();
		
		last.add(checkCreateFile());
		last.add(checkDatabase());
		last.add(checkCacheService());
		
		synchronized (LAST_STATUS) {
			LAST_STATUS.clear();
			LAST_STATUS.addAll(last);
		}
		return isStatusOK();
	}
	
	static {
		try {
			Class.forName(com.mysql.jdbc.Driver.class.getName());
		} catch (ClassNotFoundException ignored) {
		}
	}
	/**
	 * 数据库连接
	 * 
	 * @return
	 */
	protected static Status checkDatabase() {
		String name = "Database";
		try {
			Connection c = DriverManager.getConnection(
					Application.getBean(AesPreferencesConfigurer.class).getItem("db.url"), 
					Application.getBean(AesPreferencesConfigurer.class).getItem("db.user"),
					Application.getBean(AesPreferencesConfigurer.class).getItem("db.passwd"));
			SqlHelper.close(c);
		} catch (Exception ex) {
			return Status.error(name, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
		}
		return Status.success(name);
	}
	
	/**
	 * 文件权限/磁盘空间
	 * 
	 * @return
	 */
	protected static Status checkCreateFile() {
		String name = "CreateFile";
		FileWriter fw = null;
		try {
			File test = new File(FileUtils.getTempDirectory(), "ServerStatus.test");
			fw = new FileWriter(test);
			IOUtils.write(CodecUtils.randomCode(1024), fw);
			if (!test.exists()) {
				return Status.error(name, "Couldn't create file in temp Directory");
			} else {
				test.delete();
			}
			
		} catch (Exception ex) {
			return Status.error(name, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
		} finally {
			IOUtils.closeQuietly(fw);
		}
		return Status.success(name);
	}
	
	/**
	 * 缓存系统
	 * 
	 * @return
	 */
	protected static Status checkCacheService() {
		CommonCache cache = Application.getCommonCache();
		String name = "Cache/" + (cache.isUseRedis() ? "REDIS" : "EHCACHE");
		
		try {
			cache.putx("ServerStatus.test", 1, 60);
		} catch (Exception ex) {
			return Status.error(name, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
		}
		return Status.success(name);
	}
	
	// 状态
	public static class Status {
		final public String name;
		final public boolean success;
		final public String error;
		@Override
		public String toString() {
			if (success) return String.format("%s : [ OK ]", name);
			else return String.format("%s : [ ERROR ] %s", name, error);
		}
		public JSON toJson() {
			return JSONUtils.toJSONObject(name, success ? true : error);
		}
		
		private Status(String name, boolean success, String error) {
			this.name = name;
			this.success = success;
			this.error = error;
			
			if (success) {
				Application.LOG.info("Checking " + toString());
			} else {
				Application.LOG.error("Checking " + toString());
			}
		}
		private static Status success(String name) {
			return new Status(name, true, null);
		}
		private static Status error(String name, String error) {
			return new Status(name, false, error);
		}
	}
	
	// -- 
	
	/**
	 * 内存用量
	 * 
	 * @return [总计M, 已用%]
	 */
	public static double[] getHeapMemoryUsed() {
		for (MemoryInformation i : SystemUtils.getMemoryStatistics(false)) {
			if ("Heap".equalsIgnoreCase(i.getName())) {
				double t = i.getTotal();
				double p = ObjectUtils.round(i.getUsed() * 100 / t, 2);
				return new double[] { (int) (t / MemoryInformationBean.MEGABYTES), p };
			}
		}
		return new double[] { 0, 0 };
	}
}
