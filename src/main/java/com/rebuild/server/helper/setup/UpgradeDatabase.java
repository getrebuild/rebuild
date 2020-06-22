/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.setup;

import cn.devezhao.commons.ObjectUtils;
import com.rebuild.server.Application;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.SysConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * Automatically update database
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/22
 */
public final class UpgradeDatabase {
	
	private static final Log LOG = LogFactory.getLog(UpgradeDatabase.class);
	
	/**
	 * 开始升级
	 * 
	 * @throws Exception
	 */
    public void upgrade() throws Exception {
		if (Installer.isUseH2()) {
			LOG.error("H2 database unsupported upgrade!");
			return;
		}

		final Map<Integer, String[]> scripts = new UpgradeScriptReader().read();
		final int dbVer = getDbVer();
		
		int upgradeVer = dbVer;
		try {
			while (true) {
				String[] sql = scripts.get(upgradeVer + 1);
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
    public void upgradeQuietly() {
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
		String dbVer = SysConfiguration.get(ConfigurableItem.DBVer, true);
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
