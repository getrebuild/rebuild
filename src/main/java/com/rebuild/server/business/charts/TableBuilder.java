/*
rebuild - Building your system freely.
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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * HTML 表格构建
 * 
 * @author devezhao
 * @since 12/17/2018
 */
public class TableBuilder {

	private TableChart chart;
	private Object[][] rows;

	/**
	 * @param chart
	 * @param rows
	 */
	public TableBuilder(TableChart chart, Object[][] rows) {
		this.chart = chart;
		this.rows = rows;
	}

	/**
	 * @return
	 */
	public String toHTML() {
		List<Axis> axisss = new ArrayList<>();
		CollectionUtils.addAll(axisss, chart.getDimensions());
		CollectionUtils.addAll(axisss, chart.getNumericals());

		TBODY thead = new TBODY();
		TR htr = new TR();
		thead.addChild(htr);
		for (Axis axis : axisss) {
			TD td = new TD(axis.getLabel());
			htr.addChild(td);
		}

		TBODY tbody = new TBODY();
		for (Object[] row : rows) {
			TR btr = new TR();
			tbody.addChild(btr);

			for (int i = 0; i < row.length; i++) {
				String text = chart.warpAxisValue(axisss.get(i), row[i]);
				TD td = new TD(text);
				btr.addChild(td);
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

		String table = String.format("<table class=\"table table-bordered\"><thead>%s</thead><tbody>%s</tbody></table>",
				thead.toString(), tbody.toString());
		return table;
	}

	// --

	class TBODY {
		List<TR> children = new ArrayList<>();

		TBODY addChild(TR c) {
			c.parent = this;
			children.add(c);
			return this;
		}

		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			for (TR c : children) {
				sb.append(c.toString());
			}
//			return String.format("<tbody>%s</tbody>", sb.toString());
			return sb.toString();
		}
	}

	class TR {
		TBODY parent = null;
		List<TD> children = new ArrayList<>();

		TR addChild(TD c) {
			c.parent = this;
			children.add(c);
			return this;
		}

		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			for (TD c : children) {
				sb.append(c.toString());
			}
			return String.format("<tr>%s</tr>", sb.toString());
		}
	}

	class TD {
		TR parent = null;
		String content;
		int rowspan = 1;

		TD(String content) {
			this.content = StringUtils.defaultIfBlank(content, "");
		}

		@Override
		public String toString() {
			if (rowspan == 0) {
				return StringUtils.EMPTY;
			} else if (rowspan > 1) {
				return String.format("<td rowspan=\"%d\">%s</td>", rowspan, content);
			} else {
				return String.format("<td>%s</td>", content);
			}
		}
	}
}
