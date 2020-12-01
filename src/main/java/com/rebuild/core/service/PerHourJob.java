/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.distributed.DistributedJobLock;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.setup.DatabaseBackup;
import com.rebuild.utils.FileFilterByLastModified;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.LinkedHashMap;

/**
 * 每小时执行一次的 Job
 *
 * @author devezhao
 * @since 2020/2/4
 */
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

        if (hour % 2 == 0) {
            doCheckAdminDangers();
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
        FileFilterByLastModified.deletes(RebuildConfiguration.getFileOfTemp(null), 7);
    }

    public static final String CKEY_ADMIN_DANGERS = "ADMIN_DANGERS";

    /**
     * 管理员告警消息
     */
    @SuppressWarnings("unchecked")
    protected void doCheckAdminDangers() {
        LinkedHashMap<String, Object> dangers = (LinkedHashMap<String, Object>) Application.getCommonsCache().getx(CKEY_ADMIN_DANGERS);
        if (dangers == null) {
            dangers = new LinkedHashMap<>();
        }

        JSONObject checkBuild = License.siteApi("api/authority/check-build", true);
        if (checkBuild != null && checkBuild.getIntValue("build") > Application.BUILD) {
            String hasUpdate = Language.LF(
                    "NewVersion", checkBuild.getString("version"), checkBuild.getString("releaseUrl"));
            hasUpdate = hasUpdate.replace("<a ", "<a target='_blank' class='link' ");
            dangers.put("hasUpdate", hasUpdate);
        } else {
            dangers.remove("hasUpdate");
        }

        // 放入缓存
        Application.getCommonsCache().putx(CKEY_ADMIN_DANGERS, dangers, CommonsCache.TS_DAY * 2);
    }
}
