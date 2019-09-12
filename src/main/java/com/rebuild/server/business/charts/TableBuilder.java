/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.charts;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * HTML 表格构建
 * 
 * @author devezhao
 * @since 12/17/2018
 */
public class TableBuilder {

	/**
	 * 行号
	 */
	protected static final Axis LN_REF = new Axis(null, null, null, "#");
	
	private TableChart chart;
	private Object[][] rows;

	/**
	 * @param chart
	 * @param rows
	 */
	protected TableBuilder(TableChart chart, Object[][] rows) {
		this.chart = chart;
		this.rows = rows;
	}

	/**
	 * @return
	 */
	public String toHTML() {
		if (rows.length == 0) {
			return null;
		}
		
		List<Axis> axes = new ArrayList<>();
		if (chart.isShowLineNumber()) {
			axes.add(LN_REF);
		}
		CollectionUtils.addAll(axes, chart.getDimensions());
		CollectionUtils.addAll(axes, chart.getNumericals());

		TBODY thead = new TBODY("thead");
		TR ths = new TR();
		thead.addChild(ths);
		for (Axis axis : axes) {
			TD th = new TD(axis.getLabel(), "th");
			ths.addChild(th);
		}

		TBODY tbody = new TBODY();
		int isLast = rows.length + (chart.isShowSums() ? 0 : 1);
		for (Object[] row : rows) {
			isLast--;
			TR tds = new TR();
			tbody.addChild(tds);

			for (int i = 0; i < row.length; i++) {
				Axis axis = axes.get(i);
				TD td = null;
				if (axis == LN_REF) {
					td = new TD(row[i] + "", "th");
				} else {
					String text = isLast == 0
							? chart.wrapSumValue(axis, row[i])
							: chart.wrapAxisValue(axis, row[i]);
					td = new TD(text);
				}
				tds.addChild(td);
			}
		}

		// 合并纬度单元格
		for (int i = 0; i < chart.getDimensions().length; i++) {
			TD last = null;
			for (TR tr : tbody.children) {
				TD current = tr.children.get(i);
				if (last == null) {
					last = current;
					continue;
				}

				if (last.content.equals(current.content)) {
					last.rowspan++;
					current.rowspan = 0;
				} else {
					last = current;
				}
			}
		}
		
		String tClazz = (chart.isShowLineNumber() ? "line-number " : "") + (chart.isShowSums() ? "sums" : "");
		String table = String.format("<table class=\"table table-bordered %s\">%s%s</table>",
				tClazz, thead.toString(), tbody.toString());
		return table;
	}

	// --
	// HTML Table structure

	// <tbody> or <thead>
	private static class TBODY {
		private String tag = "tbody";
		private List<TR> children = new ArrayList<>();
		private TBODY() {
		}
		private TBODY(String tag) {
			this.tag = tag;
		}
		private TBODY addChild(TR c) {
			children.add(c);
			return this;
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (TR c : children) {
				sb.append(c.toString());
			}
			return String.format("<%s>%s</%s>", tag, sb.toString(), tag);
		}
	}

	// <tr>
	private static class TR {
		private List<TD> children = new ArrayList<>();
		private TR addChild(TD c) {
			children.add(c);
			return this;
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (TD c : children) {
				sb.append(c.toString());
			}
			return String.format("<tr>%s</tr>", sb.toString());
		}
	}

	// <td> or <th>
	private static class TD {
		private String tag = null;
		private String content;
		private int rowspan = 1;
		private TD(String content) {
			this(content, null);
		}
		private TD(String content, String tag) {
			this.content = StringUtils.defaultIfBlank(content, "");
			this.tag = StringUtils.defaultIfBlank(tag, "td");
		}
		@Override
		public String toString() {
			if (rowspan == 0) {
				return StringUtils.EMPTY;
			} else if (rowspan > 1) {
				return String.format("<%s rowspan=\"%d\">%s</%s>", tag, rowspan, content, tag);
			} else {
				return String.format("<%s>%s</%s>", tag, content, tag);
			}
		}
	}
}
