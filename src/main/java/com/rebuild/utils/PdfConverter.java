/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.CalendarUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.jwt.JWT;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.rebuild.core.support.ConfigurationItem.OnlyofficeJwt;
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
    public static final String TYPE_HTML = "html";

    /**
     * @param path
     * @param type
     * @return
     * @throws PdfConverterException
     */
    public static Path convert(Path path, String type) throws PdfConverterException {
        // v4.0
        if (TYPE_PDF.equalsIgnoreCase(type) && RebuildConfiguration.get(OnlyofficeServer) != null) {
            return ooConvertPdf(path);
        }

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

        if (dest.exists()) {
            if (TYPE_HTML.equalsIgnoreCase(type)) fixHtml(dest, null);
            return dest.toPath();
        }

        throw new PdfConverterException("Cannot convert to <" + type + "> : " + StringUtils.defaultIfBlank(echo, "<empty>"));
    }

    private static String TEMPALTE_HTML;
    /**
     * @param sourceHtml
     * @param title
     * @throws IOException
     */
    private static void fixHtml(File sourceHtml, String title) throws IOException {
        if (TEMPALTE_HTML == null || Application.devMode()) TEMPALTE_HTML = CommonsUtils.getStringOfRes("i18n/html-report.html");
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

    /**
     * OnlyOffice PDF
     *
     * @param path
     * @return
     * @throws PdfConverterException
     */
    public static Path ooConvertPdf(Path path) throws PdfConverterException {
        final String ooServer = RebuildConfiguration.get(OnlyofficeServer);
        final String ooJwt = RebuildConfiguration.get(OnlyofficeJwt);

        String filename = path.getFileName().toString();
        Map<String, Object> document = new HashMap<>();
        document.put("key", "key-" + filename.hashCode());
        document.put("filetype", FileUtil.getSuffix(filename));
        document.put("outputtype", "pdf");
        document.put("async", "false");
        document.put("title", filename);

        String fileUrl = String.format("/filex/download/%s?_csrfToken=%s&temp=yes",
                filename, AuthTokenManager.generateCsrfToken(90));
        fileUrl = RebuildConfiguration.getHomeUrl(fileUrl);
        document.put("url", fileUrl);

        // Token
        String token = JWT.create()
                .setPayload("document", document)
                .setExpiresAt(CalendarUtils.add(15, Calendar.MINUTE))
                .setKey(ooJwt.getBytes())
                .sign();

        Map<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put("Content-Type", "application/json");
        httpHeaders.put("Authorization", "Bearer " + token);

        String res;
        try {
            res = OkHttpUtils.post(ooServer + "/converter", JSON.toJSON(document), httpHeaders);
        } catch (IOException e) {
            throw new PdfConverterException(e);
        }
        throw new UnsupportedOperationException("TODO:" + res);
    }
}