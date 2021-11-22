/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import com.rebuild.core.Application;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * Automatically update database
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/22
 */
@Slf4j
public final class UpgradeDatabase {

    /**
     * 开始升级
     *
     * @throws Exception
     */
    public void upgrade() throws Exception {
        if (Installer.isUseH2()) {
            log.error("H2 database unsupported upgrade!");
            return;
        }

        final Map<Integer, String[]> scripts = new UpgradeScriptReader().read();
        final int currentVer = RebuildConfiguration.getInt(ConfigurationItem.DBVer);

        int upgradeVer = currentVer;

        try {
            while (true) {
                String[] sql = scripts.get(upgradeVer + 1);
                if (sql == null) {
                    break;
                } else if (sql.length == 0) {
                    upgradeVer++;
                    continue;
                }

                log.info("\n>> UPGRADE SQL (#" + (upgradeVer + 1) + ") >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n" + StringUtils.join(sql, "\n"));
                Application.getSqlExecutor().executeBatch(sql, 60 * 2);
                upgradeVer++;
            }
        } finally {
            if (currentVer != upgradeVer) {
                RebuildConfiguration.set(ConfigurationItem.DBVer, upgradeVer);
                log.info("Upgrade database version : " + upgradeVer);
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
            log.error("Upgrade database failed! Already upgraded?", ex);
        }
    }
}
