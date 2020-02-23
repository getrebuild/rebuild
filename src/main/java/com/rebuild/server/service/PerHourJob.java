/*
rebuild - Building your business-systems freely.
Copyright (C) 2020 devezhao <zhaofang123@gmail.com>

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
For more information, please see <https://getrebuild.com>
*/

package com.rebuild.server.service;

import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.setup.DatabaseBackup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Calendar;

/**
 * 每小时执行一次的 Job
 *
 * @author devezhao
 * @since 2020/2/4
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class PerHourJob extends QuartzJobBean {

    private static final Log LOG = LogFactory.getLog(PerHourJob.class);

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour == 0 && SysConfiguration.getBool((ConfigurableItem.DBBackupsEnable))) {
            doDatabaseBackup();
        }

        // others here

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
}
