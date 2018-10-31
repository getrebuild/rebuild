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

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;

import cn.devezhao.commons.ThrowableUtils;

/**
 * 各服务状态
 * 
 * @author devezhao
 * @since 10/31/2018
 */
public class ServersStatus {

	private static final Map<String, String> LAST_STATUS = new ConcurrentHashMap<>();
	static {
		LAST_STATUS.put("DataSource", EMPTY);
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
			if (e.getValue() != EMPTY) {
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
	public static boolean quickcheck() {
		String DataSource = checkingDataSource();
		Application.LOG.info("Checking DataSource : " + (DataSource == EMPTY ? "[OK]" : DataSource));
		LAST_STATUS.put("DataSource", DataSource);

		return DataSource == EMPTY;
	}

	/**
	 * @return
	 */
	protected static String checkingDataSource() {
		try {
			DataSource ds = Application.getPersistManagerFactory().getDataSource();
			Connection c = DataSourceUtils.getConnection(ds);
			DataSourceUtils.releaseConnection(c, ds);
		} catch (Exception ex) {
			return ThrowableUtils.getRootCause(ex).getLocalizedMessage();
		}
		return EMPTY;
	}
}
