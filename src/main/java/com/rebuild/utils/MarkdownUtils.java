/*
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MD 转换工具
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/16
 */
public class MarkdownUtils {

    private static final MutableDataSet OPTIONS = new MutableDataSet();

    static {
        OPTIONS.setFrom(ParserEmulationProfile.MARKDOWN)
                .set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), TaskListExtension.create()));
    }

    private static final Parser PARSER = Parser.builder(OPTIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

    /**
     * MD 渲染，支持表格，HTML 代码会转义
     *
     * @param md
     * @return
     */
    public static String render(String md) {
        md = CommonsUtils.escapeHtml(md);
        Node document = PARSER.parse(md);
        return RENDERER.render(document);
    }
}
