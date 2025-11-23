/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.support.general.DataListBuilder;
import com.rebuild.core.support.general.DataListBuilderImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * v36 数据列表
 *
 * @author devezhao
 * @since 2/27/2024
 * @see com.rebuild.core.service.dashboard.charts.builtin.DataList
 */
public class DataList2Chart extends ChartData {

    protected DataList2Chart(JSONObject config) {
        super(config);
    }

    @Override
    public JSON build() {
        Entity entity = getSourceEntity();
        Dimension[] dims = getDimensions();

        List<Object> fieldsRich = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        String sort = null;
        for (Dimension listField : dims) {
            Map<String, Object> m = DataListManager.instance.formatField(listField.getField(), listField.getParentField());
            if (listField.getLabel() != null) m.put("label", listField.getLabel());
            fieldsRich.add(m);

            String fieldPath = (String) m.get("field");
            fields.add(fieldPath);

            // 排序
            FormatSort formatSort = listField.getFormatSort();
            if (formatSort == FormatSort.ASC) sort = fieldPath + ":asc";
            else if (formatSort == FormatSort.DESC) sort = fieldPath + ":desc";
        }

        Map<String, Object> params = getExtraParams();
        JSONObject extconfig = (JSONObject) params.get("extconfig");
        if (extconfig != null && extconfig.getString("sort") != null) {
            sort = extconfig.getString("sort");
        }

        int pageSize = config.getJSONObject("option").getIntValue("pageSize");
        if (pageSize <= 0) pageSize = 40;
        if (pageSize >= 2000) pageSize = 2000;

        // fix:4.2.3 优先使用
        String filterSql = getFilterSql();

        JSONObject listConfig = new JSONObject();
        listConfig.put("pageNo", 1);
        listConfig.put("pageSize", pageSize);
        listConfig.put("reload", false);
        listConfig.put("statsField", false);
        listConfig.put("entity", entity.getName());
        listConfig.put("fields", fields);
        if (sort != null) listConfig.put("sort", sort);
        if (filterSql == null) listConfig.put("filter", config.getJSONObject("filter"));

        DataListBuilder builder = new DataListBuilderImpl423(listConfig, getUser(), filterSql);
        JSONObject data = (JSONObject) builder.getJSONResult();
        data.put("fields", fieldsRich);
        return data;
    }

    // ~
    static class DataListBuilderImpl423 extends DataListBuilderImpl {
        private final String filterSql;
        DataListBuilderImpl423(JSONObject query, ID user, String filterSql) {
            super(query, user);
            this.filterSql = filterSql;
        }

        @Override
        public String getDefaultFilter() {
            String defaultFilter = super.getDefaultFilter();
            if (filterSql == null) return defaultFilter;

            if (defaultFilter == null) return filterSql;
            return filterSql + " and " + defaultFilter;
        }
    }
}
