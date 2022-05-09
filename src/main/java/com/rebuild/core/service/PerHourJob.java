/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.SystemDiagnosis;
import com.rebuild.core.support.distributed.DistributedJobLock;
import com.rebuild.core.support.setup.DataFileBackup;
import com.rebuild.core.support.setup.DatabaseBackup;
import com.rebuild.utils.FileFilterByLastModified;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * 每小时执行一次的 Job
 *
 * @author devezhao
 * @since 2020/2/4
 */
@Slf4j
@Component
public class PerHourJob extends DistributedJobLock {

    @Scheduled(cron = "0 0 * * * ?")
    protected void executeJob() {
        if (!tryLock()) return;

        final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour == 0 && RebuildConfiguration.getBool((ConfigurationItem.DBBackupsEnable))) {
            doDatabaseBackup();
        } else if (hour == 1) {
            doCleanTempFiles();
        }

        new SystemDiagnosis().diagnose();

        // DO OTHERS HERE ...

    }

    /**
     * 数据库备份
     */
    protected void doDatabaseBackup() {
        File backups = RebuildConfiguration.getFileOfData("_backups");
        if (!backups.exists()) {
            try {
                FileUtils.forceMkdir(backups);
            } catch (IOException e) {
                log.error("Cannot mkdir `_backups`", e);
                return;
            }
        }

        try {
            new DatabaseBackup().backup(backups);
            SystemDiagnosis.setItem(SystemDiagnosis.DatabaseBackupFail, null);
        } catch (Exception e) {
            log.error("Executing [DatabaseBackup] failed!", e);
            SystemDiagnosis.setItem(SystemDiagnosis.DatabaseBackupFail, e.getLocalizedMessage());
        }

        try {
            new DataFileBackup().backup(backups);
            SystemDiagnosis.setItem(SystemDiagnosis.DataFileBackupFail, null);
        } catch (Exception e) {
            log.error("Executing [DataFileBackup] failed!", e);
            SystemDiagnosis.setItem(SystemDiagnosis.DataFileBackupFail, e.getLocalizedMessage());
        }

        int keepDays = RebuildConfiguration.getInt(ConfigurationItem.DBBackupsKeepingDays);
        if (keepDays > 0) {
            FileFilterByLastModified.deletes(backups, keepDays);
        }
    }

    /**
     * 清理临时目录
     */
    protected void doCleanTempFiles() {
        FileFilterByLastModified.deletes(RebuildConfiguration.getFileOfTemp(null), 7);
    }
}
