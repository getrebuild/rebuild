/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.OnlyOffice;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.rebuild.core.support.ConfigurationItem.OnlyofficeServer;

/**
 * Office 文件转换
 *
 * @author devezhao
 * @since 2023/2/26
 */
@Slf4j
public class PdfConverter {

    public static final String TYPE_PDF = "pdf";

    /**
     * @param path
     * @param type
     * @return
     * @throws PdfConverterException
     */
    public static Path convert(Path path, String type) throws PdfConverterException {
        try {
            // v4.0
            if (TYPE_PDF.equalsIgnoreCase(type) && RebuildConfiguration.get(OnlyofficeServer) != null) {
                try {
                    return OnlyOffice.convertPdf(path);
                } catch (Exception ooEx) {
                    log.error("oo-convert error : {}", path, ooEx);
                }
            }

            return convert(path, type, true);
        } catch (IOException e) {
            throw new PdfConverterException(e);
        }
    }

    /**
     * 转 PDF
     *
     * @param path
     * @return
     * @throws PdfConverterException
     */
    public static Path convertPdf(Path path) throws PdfConverterException {
        return convert(path, TYPE_PDF);
    }

    /**
     * @param path
     * @param type
     * @param forceRegen
     * @return
     * @throws IOException
     */
    protected static Path convert(Path path, String type, boolean forceRegen) throws IOException {
        type = StringUtils.defaultIfBlank(type, TYPE_PDF);

        final String pathFileName = path.getFileName().toString();
        final boolean isExcel = pathFileName.endsWith(".xlsx") || pathFileName.endsWith(".xls");
        // Excel 公式生效
        if (isExcel) {
            ExcelUtils.reSaveAndCalcFormula(path);
        }

        final File outDir = RebuildConfiguration.getFileOfTemp(null);
        final String outName = pathFileName.substring(0, pathFileName.lastIndexOf(".") + 1) + type;
        final File dest = new File(outDir, outName);

        if (dest.exists()) {
            if (forceRegen) FileUtils.deleteQuietly(dest);
            else return dest.toPath();
        }

        // alias
        String soffice = RebuildConfiguration.get(ConfigurationItem.LibreofficeBin);
        if (StringUtils.isBlank(soffice)) soffice = SystemUtils.IS_OS_WINDOWS ? "soffice.exe" : "libreoffice";
        String cmd = String.format("%s --headless --convert-to %s \"%s\" --outdir \"%s\"", soffice, type, path, outDir);

        String echo = CommandUtils.execFor(cmd, false);
        if (!echo.isEmpty()) log.info(echo);

        if (dest.exists()) return dest.toPath();
        throw new PdfConverterException("Cannot convert to <" + type + "> : " + StringUtils.defaultIfBlank(echo, "<empty>"));
    }
}