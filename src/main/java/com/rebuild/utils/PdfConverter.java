/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.setup.DatabaseBackup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.File;
import java.io.IOException;
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
            throw new PdfConverterException(e);
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
        String soffice = RebuildConfiguration.get(ConfigurationItem.LibreofficeBin);
        if (StringUtils.isBlank(soffice)) soffice = SystemUtils.IS_OS_WINDOWS ? "soffice.exe" : "libreoffice";
        String cmd = String.format("%s --headless --convert-to pdf \"%s\" --outdir \"%s\"", soffice, path, outdir);

        String echo = DatabaseBackup.execFor(cmd);
        if (echo.length() > 0) log.info(echo);

        if (dest.exists()) return dest.toPath();
        throw new PdfConverterException("Cannot convert to PDF : " + echo);
    }
}
