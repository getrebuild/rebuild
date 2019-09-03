/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.utils;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.Collections;

/**
 * MD 解析工具
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
	 * @param md
	 * @return
	 */
	public static String parse(String md) {
		Node document = PARSER.parse(md);
		String html = RENDERER.render(document);
		return html;
	}
}
