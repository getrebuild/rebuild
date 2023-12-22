/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;

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

    private static final Parser PARSER2;
    private static final HtmlRenderer RENDERER2;

    static {
        MutableDataSet option = new MutableDataSet();
        option.setFrom(ParserEmulationProfile.MARKDOWN).set(Parser.EXTENSIONS,
                Arrays.asList(TablesExtension.create(), TaskListExtension.create()));
        PARSER = Parser.builder(option).build();
        RENDERER = HtmlRenderer.builder(option).build();

        option = new MutableDataSet();
        option.setFrom(ParserEmulationProfile.MARKDOWN).set(Parser.EXTENSIONS,
                Arrays.asList(TablesExtension.create(), TaskListExtension.create(), MarkdownLinkAttrProvider.MarkdownLinkAttrExtension.create()));
        PARSER2 = Parser.builder(option).build();
        RENDERER2 = HtmlRenderer.builder(option).build();
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
            Node document = PARSER2.parse(md);
            return RENDERER2.render(document);
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
}
