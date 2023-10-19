/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.rebuild.core.Application;
import com.rebuild.core.service.datareport.ReportsException;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.setup.DatabaseBackup;
import com.rebuild.utils.poi.ToHtml;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.util.Assert;
import org.zwobble.mammoth.DocumentConverter;
import org.zwobble.mammoth.Result;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;

/**
 * Office 文件转换
 *
 * @author devezhao
 * @since 2023/2/26
 */
@Slf4j
public class PdfConverter {

    public static final String TYPE_PDF = "pdf";
    public static final String TYPE_HTML = "html";

    /**
     * @param path
     * @param type
     * @return
     * @throws PdfConverterException
     */
    public static Path convert(Path path, String type) throws PdfConverterException {
        try {
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
     * v34 转 HTML（样式一般）
     *
     * @param path
     * @return
     * @throws PdfConverterException
     */
    public static Path convertHtml(Path path) throws PdfConverterException {
        return convert(path, TYPE_HTML);
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

        String pathFileName = path.getFileName().toString();
        boolean isExcel = pathFileName.endsWith(".xlsx") || pathFileName.endsWith(".xls");
        boolean isWord = pathFileName.endsWith(".docx") || pathFileName.endsWith(".doc");
        // Excel 公式生效
        if (isExcel) {
            ExcelUtils.reSaveAndCalcFormula(path);
        }

        final File outdir = RebuildConfiguration.getFileOfTemp(null);
        String outName = pathFileName.substring(0, pathFileName.lastIndexOf(".") + 1) + type;
        final File dest = new File(outdir, outName);

        if (dest.exists()) {
            if (forceRegen) FileUtils.deleteQuietly(dest);
            else return dest.toPath();
        }

        if (TYPE_HTML.equalsIgnoreCase(type)) {
            if (isWord) {
                convertWord2Html(path, dest);
                return dest.toPath();
            }
            if (isExcel) {
                convertExcel2Html(path, dest);
                return dest.toPath();
            }

            throw new ReportsException("CANNOT CONVERT TO HTML : " + pathFileName);
        }

        // alias
        String soffice = RebuildConfiguration.get(ConfigurationItem.LibreofficeBin);
        if (StringUtils.isBlank(soffice)) soffice = SystemUtils.IS_OS_WINDOWS ? "soffice.exe" : "libreoffice";
        String cmd = String.format("%s --headless --convert-to %s \"%s\" --outdir \"%s\"", soffice, type, path, outdir);

        String echo = DatabaseBackup.execFor(cmd);
        if (!echo.isEmpty()) log.info(echo);

        if (dest.exists()) return dest.toPath();

        throw new PdfConverterException("Cannot convert to PDF : " + StringUtils.defaultIfBlank(echo, "<empty>"));
    }

    private static String TEMPALTE_HTML;
    /**
     * Word to HTML
     *
     * @param source
     * @param dest
     * @throws IOException
     */
    protected static void convertWord2Html(Path source, File dest) throws IOException {
        if (TEMPALTE_HTML == null || Application.devMode()) {
            TEMPALTE_HTML = CommonsUtils.getStringOfRes("i18n/html-report.html");
        }
        Assert.notNull(TEMPALTE_HTML, "TEMPALTE_HTML MISSING");

        DocumentConverter converter = new DocumentConverter();
        Result<String> result = converter.convertToHtml(source.toFile());
        String cHtml = result.getValue();
        Set<String> cWarnings = result.getWarnings();
        if (!cWarnings.isEmpty()) {
            log.warn("HTML convert warnings : {}", cWarnings);
        }

        Document html = Jsoup.parse(TEMPALTE_HTML);
        html.body().append("<div class=\"paper word\">" + cHtml + "</div>");
        html.title(source.getFileName().toString());

        FileUtils.writeStringToFile(dest, html.html(), AppUtils.UTF8);
    }

    /**
     * Word to HTML
     *
     * @param source
     * @param dest
     * @throws IOException
     */
    protected static void convertExcel2Html(Path source, File dest) throws IOException {
        if (TEMPALTE_HTML == null || Application.devMode()) {
            TEMPALTE_HTML = CommonsUtils.getStringOfRes("i18n/html-report.html");
        }
        Assert.notNull(TEMPALTE_HTML, "TEMPALTE_HTML MISSING");

        StringWriter output = new StringWriter();
        try (Workbook wb = WorkbookFactory.create(Files.newInputStream(source))) {
            ToHtml toHtml = ToHtml.create(wb, output);

            for (Iterator<Sheet> iter = wb.sheetIterator(); iter.hasNext(); ) {
                output.append("<div class=\"paper excel\">");
                toHtml.printSheet(iter.next());
                output.append("</div>");
            }
        }

        Document html = Jsoup.parse(TEMPALTE_HTML);
        html.body().append(output.toString());
        html.title(source.getFileName().toString());

        FileUtils.writeStringToFile(dest, html.html(), AppUtils.UTF8);
    }
}