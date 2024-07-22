/*!
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
import cn.devezhao.persist4j.util.support.QueryHelper;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.KVStorage;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;

/**
 * 数据库修订
 *
 * @author devezhao
 * @since 2021/11/18
 */
@Slf4j
public class DatabaseFixer {

    private static final String KEY_41 = "DataMigratorV41";
    private static final String KEY_346 = "DatabaseFixerV346";
    private static final String KEY_370 = "DatabaseFixerV370";

    /**
     * 辅助数据库升级
     */
    public static void fixIfNeed() {
        // 最新升级过的
        final int dbVer = RebuildConfiguration.getInt(ConfigurationItem.DBVer);

        if (dbVer <= 41 && !BooleanUtils.toBoolean(KVStorage.getCustomValue(KEY_41))) {
            log.info("Database fixing `#41` ...");
            ThreadPool.exec(() -> {
                try {
                    fixV41();
                    KVStorage.setCustomValue(KEY_41, "true");
                    log.info("Database fixed `#41` all succeeded");
                } catch (Exception ex) {
                    log.error("Database fixing `#41` failed : {}", ThrowableUtils.getRootCause(ex).getLocalizedMessage());
                }
            });
        }

        if (dbVer <= 52 && !BooleanUtils.toBoolean(KVStorage.getCustomValue(KEY_346))) {
            log.info("Database fixing `V346` ...");
            ThreadPool.exec(() -> {
                try {
                    fixV346();
                    KVStorage.setCustomValue(KEY_346, "true");
                    log.info("Database fixed `V346` all succeeded");
                } catch (Exception ex) {
                    log.error("Database fixing `V346` failed : {}", ThrowableUtils.getRootCause(ex).getLocalizedMessage());
                }
            });
        }

        if (dbVer <= 57 && !BooleanUtils.toBoolean(KVStorage.getCustomValue(KEY_370))) {
            log.info("Database fixing `V370` ...");
            ThreadPool.exec(() -> {
                try {
                    fixV370();
                    KVStorage.setCustomValue(KEY_370, "true");
                    log.info("Database fixed `V370` all succeeded");
                } catch (Exception ex) {
                    log.error("Database fixing `V370` failed : {}", ThrowableUtils.getRootCause(ex).getLocalizedMessage());
                }
            });
        }
    }

    // #41:多引用字段改为三方表
    private static void fixV41() {
        for (Entity entity : MetadataHelper.getEntities()) {
            if (EasyMetaFactory.valueOf(entity).isBuiltin()) continue;

            Field[] n2nFields = MetadataSorter.sortFields(entity, DisplayType.N2NREFERENCE);
            if (n2nFields.length == 0) continue;

            int count = 0;
            for (Field field : n2nFields) {
                String sql = String.format("select %s,%s from %s",
                        entity.getPrimaryField().getName(), field.getName(), entity.getName());
                Object[][] array = Application.createQueryNoFilter(sql).array();

                for (Object[] o : array) {
                    ID[] n2nIds = (ID[]) o[1];
                    if (n2nIds == null || n2nIds.length == 0) continue;

                    final Record record = EntityHelper.forNew(EntityHelper.NreferenceItem, UserService.SYSTEM_USER);
                    record.setString("belongEntity", entity.getName());
                    record.setString("belongField", field.getName());
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
                log.info("Database fixed `#41` : {} > {}", entity.getName(), count);
            }
        }
    }

    // V346:标签无效值问题
    private static void fixV346() {
        for (Entity entity : MetadataHelper.getEntities()) {
            if (EasyMetaFactory.valueOf(entity).isBuiltin()) continue;

            Field[] tagFields = MetadataSorter.sortFields(entity, DisplayType.TAG);
            if (tagFields.length == 0) continue;

            int count = 0;
            for (Field field : tagFields) {
                String usql = String.format(
                        "update `%s` set `%s` = NULL where `%s` like '[Ljava.lang.String;@%%'",
                        entity.getPhysicalName(), field.getPhysicalName(), field.getPhysicalName());
                count += Application.getSqlExecutor().execute(usql, 120);
            }

            if (count > 0) {
                log.info("Database fixed `V346` : {} > {}", entity.getName(), count);
            }
        }
    }

    // V370:补充附件文件名称
    private static void fixV370() {
        Object[][] attachs = QueryHelper.readArray(
                Application.createQueryNoFilter("select attachmentId,filePath from Attachment where fileName is null"));
        for (Object[] o : attachs) {
            String fileName = QiniuCloud.parseFileName((String) o[1]);
            Record record = EntityHelper.forUpdate((ID) o[0], UserService.SYSTEM_USER, false);
            record.setString("fileName", fileName);
            Application.getCommonsService().update(record, false);
        }
    }
}
