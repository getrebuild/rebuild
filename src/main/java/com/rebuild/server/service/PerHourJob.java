/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service;

import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.DistributedJobBean;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.setup.DatabaseBackup;
import com.rebuild.utils.FileFilterByLastModified;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

import java.util.Calendar;

/**
 * 每小时执行一次的 Job
 *
 * @author devezhao
 * @since 2020/2/4
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class PerHourJob extends DistributedJobBean {

    @Override
    protected void executeInternalSafe() throws JobExecutionException {
        final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour == 0 && SysConfiguration.getBool((ConfigurableItem.DBBackupsEnable))) {
            doDatabaseBackup();
        }
        else if (hour == 1) {
            doCleanTempFiles();
        }

        // DO OTHERS HERE ...

    }

    /**
     * 数据库备份
     */
    protected void doDatabaseBackup() {
        try {
            new DatabaseBackup().backup();
        } catch (Exception e) {
            LOG.error("Executing [DatabaseBackup] failed : " + e);
        }
    }

    /**
     * 清理临时目录
     */
    protected void doCleanTempFiles() {
        FileFilterByLastModified.deletes(SysConfiguration.getFileOfTemp(null), 7);
    }

}
