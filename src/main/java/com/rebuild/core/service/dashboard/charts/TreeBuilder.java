/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 两纬数组转树形 JSON
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/14
 */
public class TreeBuilder {

    private static final String NAME_SPEA = "--------";

    private TreemapChart chart;
    private Object[][] rows;

    /**
     * @param rows
     * @param chart
     */
    protected TreeBuilder(Object[][] rows, TreemapChart chart) {
        this.rows = rows;
        this.chart = chart;
    }

    /**
     * @return
     */
    public JSON toJSON() {
        if (rows.length == 0) {
            return JSONUtils.EMPTY_ARRAY;
        }

        int lastIndex = rows[0].length - 1;

        Map<String, TreeBuilder.Item> thereAll = new HashMap<>();
        List<TreeBuilder.Item> thereTop = new ArrayList<>();

        for (Object[] o : rows) {
            double value = (double) o[lastIndex];
            // 排除0，因为0在树图中本就不显示
            if (value <= 0d) {
                continue;
            }

            String name = (String) o[0];
            Item L1 = thereAll.get(name);
            if (L1 == null) {
                L1 = new Item(name, value);
                thereAll.put(name, L1);
                thereTop.add(L1);
            }

            Item L2 = null;
            if (lastIndex > 1) {
                name = name + NAME_SPEA + o[1];
                L2 = thereAll.get(name);
                if (L2 == null) {
                    L2 = new Item(name, value, L1);
                    thereAll.put(name, L2);
                }
            }

            Item L3;
            if (lastIndex > 2) {
                name = name + NAME_SPEA + o[2];
                L3 = thereAll.get(name);
                if (L3 == null) {
                    L3 = new Item(name, value, L2);
                    thereAll.put(name, L3);
                }
            }
        }

        JSONArray treeJson = new JSONArray();
        for (Item t : thereTop) {
            treeJson.add(t.toJson());
        }
        return treeJson;
    }

    // 单项
    private class Item {
        private Item parent;
        private List<TreeBuilder.Item> children = new ArrayList<>();
        private String name;
        private double value;

        private Item(String name, double value) {
            this(name, value, null);
        }

        private Item(String name, double value, Item parent) {
            this.name = name;
            this.value = value;
            if (parent != null) {
                this.parent = parent;
                this.parent.children.add(this);
            }
        }

        protected String getName() {
            return name;
        }

        protected double getValue() {
            if (this.children.isEmpty()) {
                return value;
            } else {
                BigDecimal t = new BigDecimal(0);
                for (Item i : this.children) {
                    t = t.add(BigDecimal.valueOf(i.getValue()));
                }
                return t.doubleValue();
            }
        }

        public JSONObject toJson() {
            JSONObject d = JSONUtils.toJSONObject(
                    new String[]{"name", "value"},
                    new Object[]{getName(), chart.wrapAxisValue(chart.getNumericals()[0], getValue())});
            if (!this.children.isEmpty()) {
                JSONArray ch = new JSONArray();
                for (Item i : this.children) {
                    ch.add(i.toJson());
                }
                d.put("children", ch);
            }
            return d;
        }
    }
}
