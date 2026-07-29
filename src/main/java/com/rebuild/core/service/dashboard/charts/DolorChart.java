/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 词云
 *
 * @author RB
 * @since 7/28/2026
 */
public class DolorChart extends ChartData {

    protected DolorChart(JSONObject config) {
        super(config);
    }

    @Override
    public JSON build() {
        Dimension[] dims = getDimensions();
        Dimension dim1 = dims[0];
        DisplayType dimType = EasyMetaFactory.getDisplayType(dim1.getField());

        Map<String, Long> counts = new LinkedHashMap<>();

        if ((dimType == DisplayType.N2NREFERENCE || dimType == DisplayType.TAG) && dim1.getParentField() == null) {
            // 多引用/标签: 数据存储在独立表，单独查询
            String entityName = getSourceEntity().getName();
            String pkName = getSourceEntity().getPrimaryField().getName();
            boolean isTag = dimType == DisplayType.TAG;
            String table = isTag ? "TagItem" : "NreferenceItem";
            String nameCol = isTag ? "tagName" : "referenceId";
            String sql = String.format(
                    "select %s, count(recordId) from %s " +
                    "where belongEntity = '%s' and belongField = '%s' " +
                    "and recordId in (select %s from %s where %s) " +
                    "group by %s",
                    nameCol, table, entityName, dim1.getField().getName(),
                    pkName, entityName, getFilterSql(), nameCol);
            Object[][] raw = Application.createQueryNoFilter(sql).array();
            for (Object[] o : raw) {
                if (o[0] == null) continue;
                String name = isTag
                        ? o[0].toString().trim()
                        : FieldValueHelper.getLabelNotry(ID.valueOf(o[0].toString()));
                if (StringUtils.isBlank(name) || ChartsHelper.VALUE_NONE.equals(name)) continue;
                counts.merge(name, ObjectUtils.toLong(o[1]), Long::sum);
            }
        } else {
            // 统一查询: select dim, count(dim) from entity where filter group by dim
            String sql = buildSql(dim1);
            Object[][] dataRaw = createQuery(sql).array();

            if (dimType == DisplayType.MULTISELECT) {
                // 多选: 解码 bitmask，将分组计数分配到各选中选项
                ConfigBean[] options = MultiSelectManager.instance.getPickListRaw(dim1.getField(), true);
                Map<Long, String> maskToText = new LinkedHashMap<>();
                for (ConfigBean option : options) {
                    maskToText.put(option.getLong("mask"), option.getString("text"));
                }
                for (Object[] o : dataRaw) {
                    long maskValue = ObjectUtils.toLong(o[0]);
                    if (maskValue <= 0) continue;
                    long groupCount = ObjectUtils.toLong(o[1]);
                    for (Map.Entry<Long, String> e : maskToText.entrySet()) {
                        if ((maskValue & e.getKey()) != 0) {
                            counts.merge(e.getValue(), groupCount, Long::sum);
                        }
                    }
                }
                // 确保所有选项都出现（即使计数为0）
                for (ConfigBean option : options) {
                    counts.putIfAbsent(option.getString("text"), 0L);
                }
            } else {
                // 普通字段: 直接使用分组结果
                for (Object[] o : dataRaw) {
                    String name = wrapAxisValue(dim1, o[0]);
                    if (StringUtils.isBlank(name) || ChartsHelper.VALUE_NONE.equals(name)) continue;
                    counts.put(name, ObjectUtils.toLong(o[1]));
                }
            }
        }

        JSONArray data = new JSONArray();
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            if (StringUtils.isBlank(e.getKey())) continue;
            data.add(JSONUtils.toJSONObject(
                    new String[]{"name", "value"},
                    new Object[]{e.getKey(), e.getValue()}));
        }

        JSONObject renderOption = config.getJSONObject("option");
        if (renderOption == null) renderOption = new JSONObject();

        return JSONUtils.toJSONObject(
                new String[]{"data", "_renderOption"},
                new Object[]{data, renderOption});
    }

    /**
     * 构建 SQL: select dim, count(dim) from entity where filter group by dim
     *
     * @param dim
     * @return
     */
    private String buildSql(Dimension dim) {
        String sql = "select {0}, count({1}) from {2} where {3} group by {0}";
        sql = MessageFormat.format(sql,
                dim.getSqlName(),
                dim.getSqlName(),
                getSourceEntity().getName(),
                getFilterSql());
        return appendSqlSort(sql);
    }
}
