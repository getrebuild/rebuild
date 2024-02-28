/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.support.general.DataListBuilder;
import com.rebuild.core.support.general.DataListBuilderImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据列表
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
        if (pageSize == 0) pageSize = 40;

        JSONObject listConfig = new JSONObject();
        listConfig.put("pageNo", 1);
        listConfig.put("pageSize", Math.max(pageSize, 1));
        listConfig.put("reload", false);
        listConfig.put("statsField", false);
        listConfig.put("entity", entity.getName());
        listConfig.put("fields", fields);
        if (sort != null) listConfig.put("sort", sort);

        DataListBuilder builder = new DataListBuilderImpl(listConfig, getUser());
        JSONObject data = (JSONObject) builder.getJSONResult();
        data.put("fields", fieldsRich);
        return data;
    }
}
