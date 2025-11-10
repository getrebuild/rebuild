/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import cn.devezhao.commons.CalendarUtils;
import com.rebuild.core.BootEnvironmentPostProcessor;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CommandUtils;
import com.rebuild.utils.CompressUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * 数据库备份
 * - `mysqldump[.exe]` 命令必须在环境变量中
 * - 除了本库还要有全局的 `RELOAD` or `FLUSH_TABLES` and `PROCESS` 权限
 *
 * @author devezhao
 * @since 2020/2/4
 */
@Slf4j
public class DatabaseBackup {

    final private String[] ignoreTables;
    final private String[] forceTables;
    // 1 --insert-ignore 2 --replace
    final private int dumpMode;

    public DatabaseBackup() {
        this(null, null, 0);
    }

    public DatabaseBackup(String[] ignoreTables, String[] forceTables, int dumpMode) {
        this.ignoreTables = ignoreTables;
        this.forceTables = forceTables;
        this.dumpMode = dumpMode;
    }

    /**
     * @return
     * @throws IOException
     */
    public File backup() throws IOException {
        File backupd = RebuildConfiguration.getFileOfData("_backups");
        if (!backupd.exists()) FileUtils.forceMkdir(backupd);

        return backup(backupd);
    }

    /**
     * @param backups
     * @return
     * @throws IOException
     */
    public File backup(File backups) throws IOException {
        String url = BootEnvironmentPostProcessor.getProperty("db.url");
        String user = BootEnvironmentPostProcessor.getProperty("db.user");
        String passwd = BootEnvironmentPostProcessor.getProperty("db.passwd");

        url = url.split("\\?")[0].split("//")[1];
        String host = url.split(":")[0];
        String port = url.split("/")[0].split(":")[1];
        String dbname = url.split("/")[1];

        String destName = "backup_database." + CalendarUtils.getPlainDateTimeFormat().format(CalendarUtils.now());
        File dest = new File(backups, destName);

        String mysqldump = RebuildConfiguration.get(ConfigurationItem.MysqldumpBin);
        if (StringUtils.isBlank(mysqldump)) mysqldump = SystemUtils.IS_OS_WINDOWS ? "mysqldump.exe" : "mysqldump";
        // https://blog.csdn.net/liaowenxiong/article/details/120587358
        // --master-data --flush-logs
        String cmd = String.format(
                "%s -u%s -p\"%s\" -h%s -P%s --default-character-set=utf8 --opt --extended-insert=true --triggers --hex-blob --single-transaction -R %s > \"%s\"",
                mysqldump, user, passwd, host, port, dbname, dest.getAbsolutePath());

        if (dumpMode == 1) {
            cmd = cmd.replaceFirst(" --opt ", " --insert-ignore --no-create-info --opt ");
        } else if (dumpMode == 2) {
            cmd = cmd.replaceFirst(" --opt ", " --replace --no-create-info --opt ");
        }

        if (!ArrayUtils.isEmpty(forceTables)) {
            String foTables = StringUtils.join(forceTables, " ");
            cmd = cmd.replaceFirst(" > ", " " + foTables + " > ");
        } else if (!ArrayUtils.isEmpty(ignoreTables)) {
            String igPrefix = " --ignore-table=" + dbname + ".";
            String ig = igPrefix + StringUtils.join(ignoreTables, igPrefix);
            cmd = cmd.replaceFirst(" -R ", " -R" + ig + " ");
        }

        String echo = CommandUtils.execFor(cmd, true);
        boolean isGotError = echo.contains("Got error");
        if (isGotError) throw new RuntimeException(echo);

        File zip = new File(backups, destName + ".zip");
        try {
            CompressUtils.forceZip(zip, dest, null);

            FileUtils.deleteQuietly(dest);
            dest = zip;
        } catch (Exception e) {
            log.warn("Cannot zip backup : {}", zip);
        }

        log.info("Backup database succeeded : {} ({})", dest, FileUtils.byteCountToDisplaySize(dest.length()));

        // 恢复
        // https://stackoverflow.com/questions/16735344/how-to-ignore-certain-mysql-tables-when-importing-a-database
        // sed -r '/INSERT INTO `(revision_history|recycle_bin|rebuild_api_request)`/d' backup_database.20240326000045 > min.sql

        return dest;
    }
}
