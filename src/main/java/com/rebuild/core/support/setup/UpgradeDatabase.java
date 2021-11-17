/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import cn.devezhao.commons.ThreadPool;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;

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
        final int currentVer = ObjectUtils.defaultIfNull(
                RebuildConfiguration.getInt(ConfigurationItem.DBVer), 0);

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

                LAST_VERS[0] = currentVer;
                LAST_VERS[1] = upgradeVer;
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

    // --

    private static final Integer[] LAST_VERS = new Integer[] { 0, 0 };
    /**
     * @return
     */
    public static void dataMigrateIfNeed() {
        if (LAST_VERS[0] < 41 && LAST_VERS[1] >= 41) {
            log.info("Data migrating #41 ...");
            ThreadPool.exec(() -> {
                try {
                    dataMigrateV41();
                } catch (Exception ex) {
                    log.error("Data migrating #41 failed : {}", ThrowableUtils.getRootCause(ex).getLocalizedMessage());
                }
            });
        }
    }

    // #41
    static void dataMigrateV41() {
        for (Entity entity : MetadataHelper.getEntities()) {
            if (EasyMetaFactory.valueOf(entity).isBuiltin()) continue;

            Field[] n2nFields = MetadataSorter.sortFields(entity, DisplayType.N2NREFERENCE);
            if (n2nFields.length == 0) continue;

            int count = 0;
            for (Field field : n2nFields) {
                String sql = String.format("select %s,%s from %s",
                        entity.getPrimaryField().getName(), field.getName(), entity.getName());
                Object[][] datas = Application.createQueryNoFilter(sql).array();

                for (Object[] o : datas) {
                    ID[] n2nIds = (ID[]) o[1];
                    if (n2nIds == null || n2nIds.length == 0) continue;

                    final Record record = EntityHelper.forNew(EntityHelper.NreferenceItem, UserService.SYSTEM_USER);
                    record.setString("belongEntity", entity.getName());
                    record.setID("recordId", (ID) o[0]);

                    for (ID n2nId : n2nIds) {
                        Record clone = record.clone();
                        clone.setID("referenceId", n2nId);
                        Application.getCommonsService().create(clone);
                    }
                    count++;
                }
            }

            if (count > 0) {
                log.info("Data migrated #41 : {} > {}", entity.getName(), count);
            }
        }
    }
}
