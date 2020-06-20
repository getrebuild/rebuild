/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.recyclebin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import com.rebuild.server.Application;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.DistributedJobBean;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import org.quartz.JobExecutionException;

import java.util.Date;

/**
 * 回收站/变更历史清理
 *
 * @author devezhao
 * @since 2019/8/21
 */
public class RecycleBinCleanerJob extends DistributedJobBean {

    // 永久保留
    private static final int KEEPING_FOREVER = 9999;

    @Override
    protected void executeInternalSafe() throws JobExecutionException {

        // 回收站

        final int rbDays = SysConfiguration.getInt(ConfigurableItem.RecycleBinKeepingDays);
        if (rbDays < KEEPING_FOREVER) {
            LOG.info("RecycleBin clean running ... " + rbDays);

            Entity entity = MetadataHelper.getEntity(EntityHelper.RecycleBin);
            Date before = CalendarUtils.addDay(-rbDays);

            String delSql = String.format("delete from `%s` where `%s` < '%s 00:00:00'",
                    entity.getPhysicalName(),
                    entity.getField("deletedOn").getPhysicalName(),
                    CalendarUtils.getUTCDateFormat().format(before));
            int del = Application.getSQLExecutor().execute(delSql, 120);
            LOG.warn("RecycleBin cleaned : " + del);

            // TODO 相关引用也在此时一并删除，因为记录已经彻底删除了

        }

        // 变更历史

        final int rhDays = SysConfiguration.getInt(ConfigurableItem.RevisionHistoryKeepingDays);
        if (rhDays < KEEPING_FOREVER) {
            LOG.info("RevisionHistory clean running ... " + rhDays);

            Entity entity = MetadataHelper.getEntity(EntityHelper.RevisionHistory);
            Date before = CalendarUtils.addDay(-rhDays);

            String delSql = String.format("delete from `%s` where `%s` < '%s 00:00:00'",
                    entity.getPhysicalName(),
                    entity.getField("revisionOn").getPhysicalName(),
                    CalendarUtils.getUTCDateFormat().format(before));
            int del = Application.getSQLExecutor().execute(delSql, 120);
            LOG.warn("RevisionHistory cleaned : " + del);
        }
    }
}
