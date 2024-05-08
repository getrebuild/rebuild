/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * MD 转换工具
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/16
 */
public class MarkdownUtils {

    private static final Parser PARSER;
    private static final HtmlRenderer RENDERER;

    private static final Parser PARSER_RICH;
    private static final HtmlRenderer RENDERER_RICH;
    private static final MutableDataSet OPTION_RICH;

    static {
        MutableDataSet option = new MutableDataSet();
        option.setFrom(ParserEmulationProfile.MARKDOWN).set(Parser.EXTENSIONS,
                Arrays.asList(TablesExtension.create(), TaskListExtension.create()));
        PARSER = Parser.builder(option).build();
        RENDERER = HtmlRenderer.builder(option).build();

        option = new MutableDataSet();
        option.setFrom(ParserEmulationProfile.MARKDOWN).set(Parser.EXTENSIONS,
                Arrays.asList(TablesExtension.create(), TaskListExtension.create(),
                        MarkdownLinkAttrProvider.MarkdownLinkAttrExtension.create(), TocExtension.create()));
        PARSER_RICH = Parser.builder(option).build();
        RENDERER_RICH = HtmlRenderer.builder(option).build();
        OPTION_RICH = option;
    }

    /**
     * MD 渲染，支持表格，HTML 代码会转义
     *
     * @param md
     * @return
     * @see #render(String, boolean, boolean)
     */
    public static String render(String md) {
        return render(md, false, false);
    }

    /**
     * MD 渲染，支持表格
     *
     * @param md
     * @param targetBlank
     * @param keepHtml HTML 代码保持
     * @return
     */
    public static String render(String md, boolean targetBlank, boolean keepHtml) {
        if (!keepHtml) {
            md = CommonsUtils.escapeHtml(md);
            md = md.replace("&gt; ", "> ");  // for MD quote
        }

        if (targetBlank) {
            Node document = PARSER_RICH.parse(md);
            return RENDERER_RICH.render(document);
        } else {
            Node document = PARSER.parse(md);
            return RENDERER.render(document);
        }
    }

    /**
     * 清理 MD 格式
     *
     * @param md
     * @return
     */
    public static String cleanMarks(String md) {
        String html = render(md);
        return Jsoup.parse(html).body().text();
    }

    /**
     * @param md
     * @param dest
     * @throws IOException
     */
    public static void md2Pdf(String md, File dest) throws IOException {
        String html = render(md, true, true);
        html = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset=\"utf-8\"/></head>" +
                "<body>" + html + "</body>" +
                "</html>";

        html2Pdf(html, dest);
    }

    /**
     * FIXME 不支持中文
     *
     * @param html
     * @param dest
     * @throws IOException
     */
    public static void html2Pdf(String html, File dest) throws IOException {
        try (OutputStream fos = Files.newOutputStream(dest.toPath())) {
            PdfConverterExtension.exportToPdf(fos, html, "", OPTION_RICH);
        }
    }
}
