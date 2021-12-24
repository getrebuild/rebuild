/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import cn.devezhao.commons.CalendarUtils;
import com.rebuild.core.BootEnvironmentPostProcessor;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CompressUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 数据库备份
 * - `mysqldump[.exe]` 命令必须在环境变量中
 * - 除了本库还要有全局的 `RELOAD` 权限
 *
 * @author devezhao
 * @since 2020/2/4
 */
@Slf4j
public class DatabaseBackup {

    /**
     * @return
     * @throws IOException
     */
    public File backup() throws IOException {
        return backup(RebuildConfiguration.getFileOfData("_backups"));
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

        String cmd = String.format(
                "mysqldump -u%s -p%s -h%s -P%s --default-character-set=utf8 --opt --extended-insert=true --triggers -R --hex-blob -x %s>%s",
                user, passwd, host, port, dbname, dest.getAbsolutePath());

        Process process;
        String encoding = "UTF-8";

        if (SystemUtils.IS_OS_WINDOWS) {
            cmd = cmd.replaceFirst("mysqldump", "cmd /c mysqldump.exe");
            process = Runtime.getRuntime().exec(cmd);
            encoding = "GBK";
        }
        // for Linux
        else {
            process = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", cmd });
        }

        BufferedReader readerError = null;
        BufferedReader reader = null;
        StringBuilder echo = new StringBuilder();
        try {
            readerError = new BufferedReader(new InputStreamReader(process.getErrorStream(), encoding));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), encoding));

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
                throw new RuntimeException(echo.toString());
            }
        } catch (InterruptedException ex) {
            log.error("command interrupted");
            throw new RuntimeException("COMMAND INTERRUPTED");
        }

        File zip = new File(backups, destName + ".zip");
        try {
            CompressUtils.forceZip(dest, zip, null);

            FileUtils.deleteQuietly(dest);
            dest = zip;
        } catch (Exception e) {
            log.warn("Cannot zip backup : {}", zip);
        }

        log.info("Backup succeeded : {} ({})", dest, FileUtils.byteCountToDisplaySize(dest.length()));

        return dest;
    }
}
