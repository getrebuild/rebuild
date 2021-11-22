/*
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
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
import com.rebuild.core.support.KVStorage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;

/**
 * @author devezhao
 * @since 2021/11/18
 */
@Slf4j
public class DataMigrator {

    private static final String KEY_41 = "DataMigratorV41";

    /**
     * 辅助数据库升级
     */
    public static void dataMigrateIfNeed() {
        if (BooleanUtils.toBoolean(KVStorage.getCustomValue(KEY_41))) {
            log.info("Data migrating #41 ...");
            ThreadPool.exec(() -> {
                try {
                    v41();
                    KVStorage.setCustomValue(KEY_41, "true");
                } catch (Exception ex) {
                    log.error("Data migrating #41 failed : {}", ThrowableUtils.getRootCause(ex).getLocalizedMessage());
                }
            });
        }
    }

    // #41 多引用字段改为三方表
    private static void v41() {
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
                log.info("Data migrated #41 : {} > {}", entity.getName(), count);
            }
        }
    }
}
