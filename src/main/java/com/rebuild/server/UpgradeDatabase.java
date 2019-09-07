/*
rebuild - Building your business-systems freely.
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

import cn.devezhao.commons.ObjectUtils;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.upgrade.DbScriptsReader;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * Automatically update database
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/22
 * 
 * @see DbScriptsReader
 */
public final class UpgradeDatabase {
	
	private static final Log LOG = LogFactory.getLog(UpgradeDatabase.class);
	
	/**
	 * 开始升级
	 * 
	 * @throws Exception
	 */
	protected void upgrade() throws Exception {
		final Map<Integer, String[]> scripts = new DbScriptsReader().read();
		final int dbVer = getDbVer();
		
		int upgradeVer = dbVer;
		try {
			while (true) {
				String sql[] = scripts.get(upgradeVer + 1);
				if (sql == null) {
					break;
				} else if (sql.length == 0) {
					upgradeVer++;
					continue;
				}
				
				LOG.info("Upgrade SQL(#" + (upgradeVer + 1) + ") > \n" + StringUtils.join(sql, "\n"));
				Application.getSQLExecutor().executeBatch(sql, 60 * 2);
				upgradeVer++;
			}
		} finally {
			if (dbVer != upgradeVer) {
				SysConfiguration.set(ConfigurableItem.DBVer, upgradeVer);
				LOG.info("Upgrade database version : " + upgradeVer);
			}
		}
	}
	
	/**
	 * 静默升级。不抛出异常
	 * 
	 * @see #upgrade()
	 */
	protected void upgradeQuietly() {
		try {
			upgrade();
		} catch (Exception ex) {
			LOG.error("Upgrade database failed! Already upgraded?", ex);
		}
	}
	
	/**
	 * @return
	 */
	public int getDbVer() {
		String dbVer = SysConfiguration.get(ConfigurableItem.DBVer);
		return ObjectUtils.toInt(dbVer, 0);
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
