/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import cn.devezhao.commons.CalendarUtils;
import com.rebuild.core.BootEnvironmentPostProcessor;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.FileFilterByLastModified;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 数据库备份
 * - `mysqldump[.exe]` 命令必须在环境变量中
 * - 除了本库还要有全局的 `RELOAD` 权限
 *
 * @author devezhao
 * @since 2020/2/4
 */
public class DatabaseBackup {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseBackup.class);

    /**
     * @return
     * @throws IOException
     */
    public File backup() throws IOException {
        String url = BootEnvironmentPostProcessor.getProperty("db.url");
        String user = BootEnvironmentPostProcessor.getProperty("db.user");
        String passwd = BootEnvironmentPostProcessor.getProperty("db.passwd");

        url = url.split("\\?")[0].split("//")[1];
        String host = url.split(":")[0];
        String port = url.split("/")[0].split(":")[1];
        String dbname = url.split("/")[1];

        String destName = dbname + "." + CalendarUtils.getPlainDateFormat().format(CalendarUtils.now());
        File backups = RebuildConfiguration.getFileOfData("_backups");
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
                LOG.error("Command failed : " + code + " # " + echo);
                return null;
            }
        } catch (InterruptedException ex) {
            LOG.error("Command interrupted");
            return null;
        }

        try {
            File zip = new File(backups, destName + ".zip");
            zip(dest, zip);

            FileUtils.deleteQuietly(dest);
            dest = zip;

        } catch (Exception ex) {
            LOG.warn(null, ex);
        }
        LOG.info("Backup succeeded : " + dest + " (" + FileUtils.byteCountToDisplaySize(dest.length()) + ")");

        // 清理
        deleteOldBackups(backups);

        return dest;
    }

    /**
     * @param backups
     */
    protected void deleteOldBackups(File backups) {
        int keepDays = RebuildConfiguration.getInt(ConfigurationItem.DBBackupsKeepingDays);
        if (keepDays < 9999) {
            FileFilterByLastModified.deletes(backups, keepDays);
        }
    }

    // --

    /**
     * ZIP 压缩（不支持目录压缩）
     *
     * @param file
     * @param dest
     */
    public static void zip(File file, File dest) throws IOException {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        ZipOutputStream zos = null;

        try {
            fos = new FileOutputStream(dest);
            bos = new BufferedOutputStream(fos);
            zos = new ZipOutputStream(bos);

            zos.putNextEntry(new ZipEntry(file.getName()));

            FileInputStream fis = null;
            BufferedInputStream bis = null;

            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);

                byte[] chunk = new byte[1024];
                int count;
                while ((count = bis.read(chunk)) != -1) {
                    zos.write(chunk, 0, count);
                }

                zos.finish();

            } finally {
                IOUtils.closeQuietly(bis);
                IOUtils.closeQuietly(fis);
            }

        } finally {
            IOUtils.closeQuietly(zos);
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(fos);
        }
    }
}
