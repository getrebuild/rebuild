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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 回收站/变更历史清理
 *
 * @author devezhao
 * @since 2019/8/21
 */
@Slf4j
@Component
public class RecycleBinCleanerJob extends DistributedJobLock {

    @Scheduled(cron = "0 0 4 * * ?")
    protected void executeJob() {
        if (!tryLock()) return;

        // 回收站

        final int rbDays = RebuildConfiguration.getInt(ConfigurationItem.RecycleBinKeepingDays);
        if (rbDays > 0) {
            log.info("RecycleBin clean running ... {}d", rbDays);

            Entity entity = MetadataHelper.getEntity(EntityHelper.RecycleBin);
            Date before = CalendarUtils.addDay(-rbDays);

            final String commonFrom = String.format(
                    "from `%s` where `%s` < '%s 00:00:00'",
                    entity.getPhysicalName(),
                    entity.getField("deletedOn").getPhysicalName(),
                    CalendarUtils.getUTCDateFormat().format(before));
            final String recordIdName = entity.getField("recordId").getPhysicalName();

            List<String> dels = new ArrayList<>();

            // 相关引用也在此时一并删除，因为记录已经彻底删除
            // Field: recordId
            String[] refs = new String[] {
                    "Attachment", "ShareAccess", "RobotApprovalStep", "NreferenceItem"
            };
            for (String refName : refs) {
                Entity refEntity = MetadataHelper.getEntity(refName);
                String refRecordIdName = "Attachment".equals(refName) ? "relatedRecord" : "recordId";

                String delRef = String.format(
                        "delete from `%s` where `%s` in ( select %s %s )",
                        refEntity.getPhysicalName(),
                        refEntity.getField(refRecordIdName).getPhysicalName(),
                        recordIdName, commonFrom);
                dels.add(delRef);
            }

            String delSql = "delete " + commonFrom;
            dels.add(delSql);

            int del = Application.getSqlExecutor().executeBatch(dels.toArray(new String[0]), 60 * 15);
            log.warn("RecycleBin cleaned : " + del);
        }

        // 变更历史

        final int rhDays = RebuildConfiguration.getInt(ConfigurationItem.RevisionHistoryKeepingDays);
        if (rhDays > 0) {
            log.info("RevisionHistory clean running ... {}d", rhDays);

            Entity entity = MetadataHelper.getEntity(EntityHelper.RevisionHistory);
            Date before = CalendarUtils.addDay(-rhDays);

            String delSql = String.format(
                    "delete from `%s` where `%s` < '%s 00:00:00'",
                    entity.getPhysicalName(),
                    entity.getField("revisionOn").getPhysicalName(),
                    CalendarUtils.getUTCDateFormat().format(before));

            int del = Application.getSqlExecutor().execute(delSql, 60 * 3);
            log.warn("RevisionHistory cleaned : " + del);
        }
    }

    /**
     * 回收站激活
     * @return
     */
    public static boolean isEnableRecycleBin() {
        return RebuildConfiguration.getInt(ConfigurationItem.RecycleBinKeepingDays) > 0;
    }

    /**
     * 修改历史激活
     * @return
     */
    public static boolean isEnableRevisionHistory() {
        return RebuildConfiguration.getInt(ConfigurationItem.RevisionHistoryKeepingDays) > 0;
    }
}
