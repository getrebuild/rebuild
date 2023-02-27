/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.rebuild.core.RebuildException;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

/**
 * @author devezhao
 * @since 2023/2/26
 */
@Slf4j
public class PdfConverter {

    /**
     * @param path
     * @return
     * @throws IOException
     */
    public static Path convert(Path path) {
        try {
            return convert(path, Boolean.FALSE);
        } catch (IOException e) {
            throw new RebuildException(e);
        }
    }

    /**
     * @param path
     * @param forceRegen
     * @return
     * @throws IOException
     */
    public static Path convert(Path path, boolean forceRegen) throws IOException {
        final File outdir = RebuildConfiguration.getFileOfTemp(null);
        String pdfName = path.getFileName().toString();
        pdfName = pdfName.substring(0, pdfName.lastIndexOf(".")) + ".pdf";
        final File dest = new File(outdir, pdfName);

        if (dest.exists()) {
            if (forceRegen) FileUtils.deleteQuietly(dest);
            else return dest.toPath();
        }

        // alias
        String soffice = SystemUtils.IS_OS_WINDOWS ? "soffice.exe" : "libreoffice";
        String cmd = String.format("%s --headless --convert-to pdf \"%s\" --outdir \"%s\"", soffice, path, outdir);

        ProcessBuilder builder = new ProcessBuilder();
        String encoding = "UTF-8";

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

        if (echo.length() > 0) log.info(echo.toString());

        if (dest.exists()) return dest.toPath();
        throw new RebuildException("Cannot convert to PDF : " + echo);
    }
}
