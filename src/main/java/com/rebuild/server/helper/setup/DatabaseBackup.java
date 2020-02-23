/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.setup;

import cn.devezhao.commons.CalendarUtils;
import com.rebuild.server.Application;
import com.rebuild.server.helper.AesPreferencesConfigurer;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.MaxBackupIndexDailyRollingFileAppender;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * 数据库备份
 *
 * @author devezhao
 * @since 2020/2/4
 */
public class DatabaseBackup {

    private static final Log LOG = LogFactory.getLog(DatabaseBackup.class);

    /**
     * @return
     * @throws IOException
     */
    public File backup() throws IOException {
        String url = Application.getBean(AesPreferencesConfigurer.class).getItem("db.url");
        String user = Application.getBean(AesPreferencesConfigurer.class).getItem("db.user");
        String passwd = Application.getBean(AesPreferencesConfigurer.class).getItem("db.passwd");

        url = url.split("\\?")[0].split("//")[1];
        String host = url.split(":")[0];
        String port = url.split("/")[0].split(":")[1];
        String dbname = url.split("/")[1];

        String destName = dbname + "." + CalendarUtils.getPlainDateFormat().format(CalendarUtils.now());
        File backups = SysConfiguration.getFileOfData("backups");
        if (!backups.exists()) {
            FileUtils.forceMkdir(backups);
        }
        File dest = new File(backups, destName);

        String cmd = String.format(
                "mysqldump -u%s -p%s -h%s -P%s --default-character-set=utf8 --opt --extended-insert=true --triggers -R --hex-blob -x %s>%s",
                user, passwd, host, port, dbname, dest.getAbsolutePath());

        Process process;

        if (SystemUtils.IS_OS_WINDOWS) {
            cmd = cmd.replaceFirst("mysqldump", "cmd /c mysqldump.exe");
            process = Runtime.getRuntime().exec(cmd);
        }
        // for Linux
        else {
            process = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", cmd });
        }

        BufferedReader readerError = null;
        BufferedReader reader = null;
        StringBuilder echo = new StringBuilder();
        try {
            readerError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = readerError.readLine()) != null) {
                echo.append(line).append("\n");
            }
            while ((line = reader.readLine()) != null) {
                echo.append(line).append("\n");
            }

        } finally {
            IOUtils.closeQuietly(readerError);
            IOUtils.closeQuietly(reader);
            process.destroy();
        }

        boolean isGotError = echo.toString().contains("Got error");
        try {
            int code = process.waitFor();
            if (code != 0 || isGotError) {
                LOG.error("Command failed : " + code + " # " + echo.toString());
                return null;
            }
        } catch (InterruptedException ex) {
            LOG.error("Command interrupted");
            return null;
        }

        try {
            File zip = new File(backups, destName + ".zip");
            CommonsUtils.zip(dest, zip);

            FileUtils.deleteQuietly(dest);
            dest = zip;

        } catch (Exception ex) {
            LOG.warn(null, ex);
        }
        LOG.info("Backup succeeded : " + dest + " (" + FileUtils.byteCountToDisplaySize(dest.length()) + ")");

        deleteOldBackups(backups, SysConfiguration.getInt(ConfigurableItem.DBBackupsKeepingDays));

        return dest;
    }

    /**
     * @param backupDir
     * @param maxKeep
     */
    protected void deleteOldBackups(File backupDir, int maxKeep) {
        if (maxKeep <= 0) {
            return;
        }

        File[] backupFiles = backupDir.listFiles();
        if (backupFiles == null) {
            return;
        }

        Arrays.sort(backupFiles, new MaxBackupIndexDailyRollingFileAppender.CompratorByLastModified());
        for (int i = maxKeep; i < backupFiles.length;  i++) {
            File file = backupFiles[i];
            FileUtils.deleteQuietly(file);
        }
    }
}
