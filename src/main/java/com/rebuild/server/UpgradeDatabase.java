/*
rebuild - Building your system freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.helper.ConfigItem;
import com.rebuild.server.helper.SystemConfig;
import com.rebuild.server.helper.upgrade.DbScriptsReader;

/**
 * Automatically update SQL scripts
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/22
 */
public class UpgradeDatabase {
	
	private static final Log LOG = LogFactory.getLog(UpgradeDatabase.class);
	
	/**
	 * @throws SQLException
	 */
	protected void upgrade() throws Exception {
		final Map<Integer, String> scripts = new DbScriptsReader().read();
		final int dbVer = getDbVer();
		
		int verNo = dbVer;
		try {
			while (true) {
				String sql = scripts.get(verNo + 1);
				if (sql == null) {
					break;
				} else if (StringUtils.isBlank(sql)) {
					verNo++;
					continue;
				}
				
				Application.getSQLExecutor().execute(sql, 60 * 1);
				verNo++;
			}
		} finally {
			if (dbVer != verNo) {
				SystemConfig.set(ConfigItem.DBVer, verNo);
				LOG.info("Upgrade database version : " + verNo);
			}
		}
	}
	
	/**
	 */
	protected void upgradeQuietly() {
		try {
			upgrade();
		} catch (Exception ex) {
			LOG.error("Upgrade database failure!", ex);
		}
	}
	
	/**
	 * @return
	 */
	public int getDbVer() {
		return (int) SystemConfig.getLong(ConfigItem.DBVer, 0);
	}
	
	private static final UpgradeDatabase INSTANCE = new UpgradeDatabase();
	private UpgradeDatabase() {
	}
	
	/**
	 * @return
	 */
	public static UpgradeDatabase getInstance() {
		return INSTANCE;
	}
}
