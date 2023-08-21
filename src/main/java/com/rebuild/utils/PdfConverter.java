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
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
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
            return convert(path, type, Boolean.FALSE);
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

        // 打开并保存以便公式生效
        try (Workbook wb = WorkbookFactory.create(Files.newInputStream(path))) {
            wb.setForceFormulaRecalculation(true);
            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();

            try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
                wb.write(fos);
            }
        } catch (IOException e) {
            throw new PdfConverterException(e);
        }

        final File outdir = RebuildConfiguration.getFileOfTemp(null);
        String pdfName = path.getFileName().toString();
        pdfName = pdfName.substring(0, pdfName.lastIndexOf(".") + 1) + type;
        final File dest = new File(outdir, pdfName);

        if (dest.exists()) {
            if (forceRegen) FileUtils.deleteQuietly(dest);
            else return dest.toPath();
        }

        // alias
        String soffice = RebuildConfiguration.get(ConfigurationItem.LibreofficeBin);
        if (StringUtils.isBlank(soffice)) soffice = SystemUtils.IS_OS_WINDOWS ? "soffice.exe" : "libreoffice";
        String cmd = String.format("%s --headless --convert-to %s \"%s\" --outdir \"%s\"", soffice, type, path, outdir);

        String echo = DatabaseBackup.execFor(cmd);
        if (!echo.isEmpty()) log.info(echo);

        if (dest.exists()) {
            if (TYPE_HTML.equalsIgnoreCase(type)) fixHtml(dest, null);
            return dest.toPath();
        }

        throw new PdfConverterException("Cannot convert to PDF : " + StringUtils.defaultIfBlank(echo, "<empty>"));
    }

    private static String TEMPALTE_HTML;
    /**
     * @param sourceHtml
     * @param title
     * @throws IOException
     */
    private static void fixHtml(File sourceHtml, String title) throws IOException {
        if (TEMPALTE_HTML == null) TEMPALTE_HTML = CommonsUtils.getStringOfRes("i18n/html-report.html");
        if (TEMPALTE_HTML == null) return;

        final Document template = Jsoup.parse(TEMPALTE_HTML);
        final Element body = template.body();

        final Document source = Jsoup.parse(sourceHtml);

        // 提取表格
        for (Element table : source.select("body>table")) {
            Element page = body.appendElement("div").addClass("page");
            page.appendChild(table);
        }

        // 图片添加 temp=yes
        for (Element img : body.select("img")) {
            String src = img.attr("src");
            if (!src.startsWith("data:")) {
                img.attr("src", src + "?temp=yes");
            }
        }

        // TITLE
        if (title == null) title = sourceHtml.getName();
        Objects.requireNonNull(template.head().selectFirst("title")).text(title);

        FileUtils.writeStringToFile(sourceHtml, template.html(), "UTF-8");
    }
}
