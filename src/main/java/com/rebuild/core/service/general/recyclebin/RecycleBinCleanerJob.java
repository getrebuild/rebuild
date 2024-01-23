/*!
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
import org.springframework.core.NamedThreadLocal;
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

            // 相关系统引用也在此时一并删除，因为记录已经彻底删除
            // Field: recordId
            String[] sysRefs = new String[] {
                    "Attachment", "ShareAccess", "RobotApprovalStep", "NreferenceItem", "TagItem"
            };
            for (String refName : sysRefs) {
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
            log.warn("RecycleBin cleaned : {}", del);
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
            log.warn("RevisionHistory cleaned : {}", del);
        }

        // CommonLog 保留6个月

        Entity entity = MetadataHelper.getEntity(EntityHelper.CommonsLog);
        String delSql = String.format(
                "delete from `%s` where `%s` < '%s 00:00:00'",
                entity.getPhysicalName(),
                entity.getField("logTime").getPhysicalName(),
                CalendarUtils.getUTCDateFormat().format(CalendarUtils.addMonth(-6)));
        Application.getSqlExecutor().execute(delSql, 60 * 3);
    }

    // --

    private static final ThreadLocal<Boolean> SKIP_RECYCLEBIN = new NamedThreadLocal<>("Skip recycle-bin");
    private static final ThreadLocal<Boolean> SKIP_REVISIONHISTORY = new NamedThreadLocal<>("Skip revision history");

    /**
     * 回收站是否激活
     * @return
     * @see #setSkipRecyclebinOnce()
     */
    public static boolean isEnableRecycleBin() {
        boolean enable = RebuildConfiguration.getInt(ConfigurationItem.RecycleBinKeepingDays) > 0;
        if (enable) {
            Boolean skip = SKIP_RECYCLEBIN.get();
            SKIP_RECYCLEBIN.remove();
            if (skip != null && skip) return false;
        }
        return enable;
    }

    /**
     * 变更历史是否激活
     * @return
     * @see #setSkipRevisionHistoryOnce()
     */
    public static boolean isEnableRevisionHistory() {
        boolean enable = RebuildConfiguration.getInt(ConfigurationItem.RevisionHistoryKeepingDays) > 0;
        if (enable) {
            Boolean skip = SKIP_REVISIONHISTORY.get();
            SKIP_REVISIONHISTORY.remove();
            if (skip != null && skip) return false;
        }
        return enable;
    }

    /**
     * 跳过一次回收站
     */
    public static void setSkipRecyclebinOnce() {
        SKIP_RECYCLEBIN.set(true);
    }

    /**
     * 跳过一次变更历史
     */
    public static void setSkipRevisionHistoryOnce() {
        SKIP_REVISIONHISTORY.set(true);
    }
}
