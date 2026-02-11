/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.recyclebin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.distributed.DistributedJobLock;
import com.rebuild.core.support.integration.QiniuCloud;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.core.NamedThreadLocal;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        final Set<String> deletePaths42 = new HashSet<>();
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
            String[] sysRefs = new String[]{
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

                // 4.2 删除附件
                if ("Attachment".equals(refName)) {
                    String sql = "select FILE_PATH " + delRef.substring(7);
                    Object[][] array = Application.getQueryFactory().createNativeQuery(sql).array();
                    for (Object[] o : array) {
                        deletePaths42.add((String) o[0]);
                    }
                }
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

        // CommonLog 保留 90d

        Entity entity = MetadataHelper.getEntity(EntityHelper.CommonsLog);
        String delSql = String.format(
                "delete from `%s` where `%s` < '%s 00:00:00'",
                entity.getPhysicalName(),
                entity.getField("logTime").getPhysicalName(),
                CalendarUtils.getUTCDateFormat().format(CalendarUtils.addDay(-90)));
        Application.getSqlExecutor().execute(delSql, 60 * 3);

        // 4.2 已删除附件*最少*保留 90d

        List<ID> deletesAuto42 = new ArrayList<>();
        Object[][] array = Application.createQueryNoFilter(
                "select attachmentId,filePath from Attachment where isDeleted = 'T' and modifiedOn < ?")
                .setParameter(1, CalendarUtils.addDay(-Math.max(rbDays, 90)))
                .array();
        for (Object[] o : array) {
            deletesAuto42.add((ID) o[0]);
            deletePaths42.add((String) o[1]);
        }

        // 删记录
        if (!deletesAuto42.isEmpty()) {
            Application.getCommonsService().delete(deletesAuto42.toArray(new ID[0]), false);
        }
        // 删文件
        for (String path : deletePaths42) {
            Object[] o = Application.createQueryNoFilter(
                    "select count(filePath) from Attachment where filePath = ?")
                    .setParameter(1, path)
                    .unique();
            // 检查附件是否有其他字段使用（例如记录转换、触发器处理的）
            if (o != null && ObjectUtils.toInt(o[0]) > 0) continue;

            boolean s = false;
            if (QiniuCloud.instance().available()) {
                try {
                    s = QiniuCloud.instance().delete(path);
                } catch (Exception ignored) {}
            } else {
                s = FileUtils.deleteQuietly(RebuildConfiguration.getFileOfData(path));
            }
            log.info("File/Attachment deleted : {} >> {}", path, s);
        }
    }

    // --

    private static final ThreadLocal<Boolean> SKIP_RECYCLEBIN = new NamedThreadLocal<>("Skip recycle-bin");
    private static final ThreadLocal<Boolean> SKIP_REVISIONHISTORY = new NamedThreadLocal<>("Skip revision history");

    /**
     * 回收站是否激活
     *
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
     *
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
