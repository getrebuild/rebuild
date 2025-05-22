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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

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
    public static Path convert(Path path, String type, boolean forceRegen) throws IOException {
        type = StringUtils.defaultIfBlank(type, TYPE_PDF);

        final String filename = path.getFileName().toString();
        final File outDir = RebuildConfiguration.getFileOfTemp(null);
        final String outName = filename.substring(0, filename.lastIndexOf(".") + 1) + type;
        final File dest = new File(outDir, outName);

        if (dest.exists()) {
            if (forceRegen) FileUtils.deleteQuietly(dest);
            else return dest.toPath();
        }

        // Excel 公式生效
        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            ExcelUtils.reSaveAndCalcFormula(path);
        }

        // alias
        String soffice = RebuildConfiguration.get(ConfigurationItem.LibreofficeBin);
        if (StringUtils.isBlank(soffice)) soffice = SystemUtils.IS_OS_WINDOWS ? "soffice.exe" : "libreoffice";
        String cmd = String.format("%s --headless --convert-to %s \"%s\" --outdir \"%s\"", soffice, type, path, outDir);

        String echo = CommandUtils.execFor(cmd, false);
        if (!echo.isEmpty()) log.info(echo);

        if (dest.exists()) return dest.toPath();

        String error = "CANNOT CONVERT " + type.toUpperCase();
        if (StringUtils.isNotBlank(echo)) error += " : " + echo;
        throw new PdfConverterException(error);
    }

    /**
     * 元数据
     *
     * @param file
     * @param newFileName
     */
    public static File reSavePdf4Meta(File file, String newFileName) {
        PDDocument origin = null;
        PDDocument cleand = null;
        try {
            origin = Loader.loadPDF(file);
            cleand = new PDDocument();
            origin.getPages().forEach(cleand::addPage);

            PDDocumentInformation newinfo = new PDDocumentInformation();
            newinfo.setTitle(StringUtils.defaultIfBlank(newFileName, file.getName()));
            newinfo.setAuthor("REBUILD");
            newinfo.setCreator("PdfConverter");
            cleand.setDocumentInformation(newinfo);

            FileUtils.deleteQuietly(file);
            if (newFileName != null) {
                file = new File(file.getParent(), newFileName);
            }
            cleand.save(file);

        } catch (Exception ex) {
            log.warn("PDF resave error : {}", ex.getLocalizedMessage());
        } finally {
            IOUtils.closeQuietly(origin, cleand);
        }
        return file;
    }
}