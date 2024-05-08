/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author devezhao
 * @since 2024/4/30
 */
@Slf4j
public class CommandUtils {

    /**
     * 执行命令行
     *
     * @param cmd
     * @return
     * @throws IOException
     */
    public static String execFor(String cmd) throws IOException {
        ProcessBuilder builder = new ProcessBuilder();
        String encoding = "UTF-8";

        log.info("CMD : {}", cmd);

        if (SystemUtils.IS_OS_WINDOWS) {
            builder.command("cmd.exe", "/c", cmd);
            encoding = "GBK";
        } else {
            // for Linux/Unix
            builder.command("/bin/sh", "-c", cmd);
        }

        builder.redirectErrorStream(true);
        Process process = builder.start();

        BufferedReader reader = null;
        StringBuilder echo = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), encoding));

            String line;
            while ((line = reader.readLine()) != null) {
                echo.append(line).append("\n");
            }

        } finally {
            IOUtils.closeQuietly(reader);
            process.destroy();
        }

        try {
            int code = process.waitFor();
            if (code != 0) {
                throw new RuntimeException(code + "#" + echo);
            }
        } catch (InterruptedException ex) {
            log.error("command interrupted");
            throw new RuntimeException("COMMAND INTERRUPTED");
        }

        return echo.toString();
    }
}
