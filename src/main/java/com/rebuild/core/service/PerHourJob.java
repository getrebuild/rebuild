/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.ServerStatus;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.SysbaseHeartbeat;
import com.rebuild.core.support.distributed.DistributedJobLock;
import com.rebuild.core.support.setup.DatabaseBackup;
import com.rebuild.core.support.setup.DatafileBackup;
import com.rebuild.utils.FileFilterByLastModified;
import com.rebuild.utils.OshiUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * 每小时执行一次的 Job
 *
 * @author devezhao
 * @since 2020/2/4
 */
@Slf4j
@Component
public class PerHourJob extends DistributedJobLock {

    @Scheduled(cron = "45 0 * * * ?")
    protected void executeJob() {
        if (!tryLock()) return;

        final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour == 0 && RebuildConfiguration.getBool((ConfigurationItem.DBBackupsEnable))) {
            doBackups();
        } else if (hour == 1) {
            doCleanTempFiles();
            doCleanExpiredSharedUrls();
            doCleanSystemRefs();
        }

        new SysbaseHeartbeat().heartbeat();

        // DO OTHERS HERE ...

    }

    /**
     * 执行备份
     */
    protected void doBackups() {
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
            SysbaseHeartbeat.setItem(SysbaseHeartbeat.DatabaseBackupFail, null);
        } catch (Exception e) {
            log.error("Executing [DatabaseBackup] failed!", e);
            SysbaseHeartbeat.setItem(SysbaseHeartbeat.DatabaseBackupFail, e.getLocalizedMessage());
        }

        try {
            new DatafileBackup().backup(backups);
            SysbaseHeartbeat.setItem(SysbaseHeartbeat.DataFileBackupFail, null);
        } catch (Exception e) {
            log.error("Executing [DataFileBackup] failed!", e);
            SysbaseHeartbeat.setItem(SysbaseHeartbeat.DataFileBackupFail, e.getLocalizedMessage());
        }

        int keepDays = RebuildConfiguration.getInt(ConfigurationItem.DBBackupsKeepingDays);
        if (keepDays > 0) {
            FileFilterByLastModified.deletes(backups, keepDays);
        }
    }

    /**
     * 清理临时目录/文件
     */
    protected void doCleanTempFiles() {
        FileFilterByLastModified.deletes(RebuildConfiguration.getFileOfTemp(null), 7);
    }

    /**
     * 清理过期共享文件
     * @see com.rebuild.core.support.ShortUrls
     */
    protected void doCleanExpiredSharedUrls() {
        String dsql = String.format(
                "delete from `short_url` where `EXPIRE_TIME` < '%s'",
                CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now()));
        int a = Application.getSqlExecutor().execute(dsql, 600);
        log.info("Clean expired shared urls : {}", a);
    }

    /**
     * TODO 清理系统级引用
     */
    protected void doCleanSystemRefs() {
    }

    // --

    @Scheduled(fixedRate = 300000, initialDelay = 300000)
    protected void executeJobPer5min() {
        if (Application.devMode()) return;
        JSONObject res = License.siteApi("api/ucenter/bind-query");
        if (StringUtils.isBlank(res.getString("bindAccount"))) return;

        Map<String, Object> map = new HashMap<>();
        map.put("ok", ServerStatus.isStatusOK());
        map.put("memjvm", OshiUtils.getJvmMemoryUsed());
        map.put("mem", OshiUtils.getOsMemoryUsed());
        map.put("load", OshiUtils.getSystemLoad());

        String data = JSON.toJSONString(map);
        String apiUrl = "api/ucenter/data-echo?data=" + CodecUtils.urlEncode(data);
        License.siteApiNoCache(apiUrl);
    }
}
