/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.commons.ObjectUtils;
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
    protected static final Axis LN_REF = new Axis(null, null, null, "#", null);

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
                TD td;
                if (axis == LN_REF) {
                    td = new TD(row[i] + "", "th");
                } else {
                    String text;
                    if (isLast == 0) {
                        text = chart.wrapSumValue(axis, row[i]);
                    } else if (axis instanceof Numerical) {
                        text = chart.wrapAxisValue((Numerical) axis, row[i], true);
                    } else {
                        text = chart.wrapAxisValue((Dimension) axis, row[i], true);
                    }
                    td = new TD(text);
                }
                tds.addChild(td);
            }
        }

        // 合并纬度单元格
        if (chart.isMergeCell()) {
            for (int i = 0; i < chart.getDimensions().length; i++) {
                // 行号
                if (chart.isShowLineNumber() && i == 0) continue;

                TD last = null;
                int sumMinus = 0;  // 合并的单元格
                int childrenLen = tbody.children.size();
                for (TR tr : tbody.children) {
                    TD current = tr.children.get(i);
                    if (last == null || childrenLen-- <= 1) {
                        last = current;
                        continue;
                    }

                    if (last.content.equals(current.content)) {
                        last.rowspan++;
                        current.rowspan = 0;
                        sumMinus++;
                    } else {
                        last = current;
                    }
                }

                if (chart.isShowSums() && sumMinus > 0 && StringUtils.isNotBlank(last.content)) {
                    int num = ObjectUtils.toInt(last.content) - sumMinus;
                    last.content = String.valueOf(num);
                }
            }
        }

        String tClazz = (chart.isShowLineNumber() ? "line-number " : "") + (chart.isShowSums() ? "sums" : "");
        return String.format("<table class=\"table table-bordered %s\">%s%s</table>", tClazz, thead, tbody);
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
            return String.format("<%s>%s</%s>", tag, sb, tag);
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
            return String.format("<tr>%s</tr>", sb);
        }
    }

    // <td> or <th>
    private static class TD {

        private String tag;
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
