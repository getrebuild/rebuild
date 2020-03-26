/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.Collections;

/**
 * MD 转换工具
 * 
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/16
 */
public class MarkdownUtils {

	private static final MutableDataSet OPTIONS = new MutableDataSet();
	static {
		OPTIONS.set(Parser.EXTENSIONS, Collections.singletonList(TablesExtension.create()));
//		OPTIONS.set(HtmlRenderer.SOFT_BREAK, "<br/>");
	}
	private static final Parser PARSER = Parser.builder(OPTIONS).build();
	private static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

	/**
     * MD 渲染，支持表格
     *
	 * @param md
	 * @return
	 */
	public static String render(String md) {
		Node document = PARSER.parse(md);
		return RENDERER.render(document);
	}
}
