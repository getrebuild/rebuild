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

import static org.apache.commons.lang.StringUtils.EMPTY;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;

import com.rebuild.server.helper.SystemConfigurer;

import cn.devezhao.commons.ThrowableUtils;

/**
 * 各服务状态
 * 
 * @author devezhao
 * @since 10/31/2018
 */
public class ServerStatus {

	private static final Map<String, String> LAST_STATUS = new ConcurrentHashMap<>();
	static {
		LAST_STATUS.put("DataSource", EMPTY);
		LAST_STATUS.put("CreateFile", EMPTY);
		LAST_STATUS.put("StroageService", EMPTY);
		LAST_STATUS.put("CacheService", EMPTY);
	}
	/**
	 * 最近检查状态
	 * 
	 * @return
	 */
	public static Map<String, String> getLastStatus() {
		return Collections.unmodifiableMap(LAST_STATUS);
	}
	/**
	 * 服务正常
	 * 
	 * @return
	 */
	public static boolean isStatusOK() {
		for (Map.Entry<String, String> e : getLastStatus().entrySet()) {
			if (StringUtils.isNotBlank(e.getValue())) {
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
		String theDataSource = checkDataSource();
		Application.LOG.info("Checking DataSource : " + StringUtils.defaultIfBlank(theDataSource, "[ OK ]"));
		LAST_STATUS.put("DataSource", theDataSource);
		
		String theCreateFile = checkCreateFile();
		Application.LOG.info("Checking CreateFile : " + StringUtils.defaultIfBlank(theCreateFile, "[ OK ]"));
		LAST_STATUS.put("CreateFile", theCreateFile);
		
		return isStatusOK();
	}

	/**
	 * @return
	 */
	protected static String checkDataSource() {
		try {
			DataSource ds = Application.getPersistManagerFactory().getDataSource();
			Connection c = DataSourceUtils.getConnection(ds);
			DataSourceUtils.releaseConnection(c, ds);
		} catch (Exception ex) {
			return ThrowableUtils.getRootCause(ex).getLocalizedMessage();
		}
		return EMPTY;
	}
	
	/**
	 * @return
	 */
	protected static String checkCreateFile() {
		FileWriter fw = null;
		try {
			File test = SystemConfigurer.getFileOfTemp("test");
			fw = new FileWriter(test);
			IOUtils.write("TestCreateFile", fw);
			if (!test.exists()) {
				return "Cloud't create file in temp Directory";
			} else {
				test.delete();
			}
			
		} catch (Exception ex) {
			return ThrowableUtils.getRootCause(ex).getLocalizedMessage();
		} finally {
			IOUtils.closeQuietly(fw);
		}
		return EMPTY;
	}
}
