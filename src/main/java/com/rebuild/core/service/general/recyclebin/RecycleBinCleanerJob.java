/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.recyclebin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.distributed.DistributedJobLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 回收站/变更历史清理
 *
 * @author devezhao
 * @since 2019/8/21
 */
@Slf4j
@Component
public class RecycleBinCleanerJob extends DistributedJobLock {

    // 永久保留
    private static final int KEEPING_FOREVER = 9999;

    @Scheduled(cron = "0 0 4 * * ?")
    protected void executeJob() {
        if (!tryLock()) return;

        // 回收站

        final int rbDays = RebuildConfiguration.getInt(ConfigurationItem.RecycleBinKeepingDays);
        if (rbDays < KEEPING_FOREVER) {
            log.info("RecycleBin clean running ... " + rbDays);

            Entity entity = MetadataHelper.getEntity(EntityHelper.RecycleBin);
            Date before = CalendarUtils.addDay(-rbDays);

            String delSql = String.format("delete from `%s` where `%s` < '%s 00:00:00'",
                    entity.getPhysicalName(),
                    entity.getField("deletedOn").getPhysicalName(),
                    CalendarUtils.getUTCDateFormat().format(before));
            int del = Application.getSqlExecutor().execute(delSql, 120);
            log.warn("RecycleBin cleaned : " + del);

            // TODO 相关引用也在此时一并删除，因为记录已经彻底删除了

        }

        // 变更历史

        final int rhDays = RebuildConfiguration.getInt(ConfigurationItem.RevisionHistoryKeepingDays);
        if (rhDays < KEEPING_FOREVER) {
            log.info("RevisionHistory clean running ... " + rhDays);

            Entity entity = MetadataHelper.getEntity(EntityHelper.RevisionHistory);
            Date before = CalendarUtils.addDay(-rhDays);

            String delSql = String.format("delete from `%s` where `%s` < '%s 00:00:00'",
                    entity.getPhysicalName(),
                    entity.getField("revisionOn").getPhysicalName(),
                    CalendarUtils.getUTCDateFormat().format(before));
            int del = Application.getSqlExecutor().execute(delSql, 120);
            log.warn("RevisionHistory cleaned : " + del);
        }
    }
}
