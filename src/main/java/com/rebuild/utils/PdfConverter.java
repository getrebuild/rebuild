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

/**
 * @author devezhao
 * @since 2023/2/26
 */
@Slf4j
public class PdfConverter {

    public static final String TYPE_PDF = "pdf";
    public static final String TYPE_HTML = "html";

    /**
     * 转 PDF
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static Path convert(Path path) {
        try {
            return convert(path, TYPE_PDF, Boolean.FALSE);
        } catch (IOException e) {
            throw new PdfConverterException(e);
        }
    }

    /**
     * v34 转 HTML（样式一般）
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static Path convertHtml(Path path) {
        try {
            return convert(path, TYPE_HTML, Boolean.FALSE);
        } catch (IOException e) {
            throw new PdfConverterException(e);
        }
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
            if (TYPE_HTML.equalsIgnoreCase(type)) fixHtml(dest);
            return dest.toPath();
        }

        throw new PdfConverterException("Cannot convert to PDF : " + StringUtils.defaultIfBlank(echo, "<empty>"));
    }

    private static void fixHtml(File htmlFile) throws IOException {
        final Document html = Jsoup.parse(htmlFile);

        // 基础样式
        html.head().append("<link rel=\"stylesheet\" type=\"text/css\" href=\"../../assets/css/html-report.css?v=3.4\" />");

        // 图片添加 `temp=yes`
        for (Element img : html.body().select("img")) {
            String src = img.attr("src");
            if (!src.startsWith("data:")) {
                img.attr("src", src + "?temp=yes");
            }
        }

        FileUtils.writeStringToFile(htmlFile, html.html(), "UTF-8");
    }
}
