/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserFilters;
import com.rebuild.core.service.dashboard.charts.ChartsHelper;
import com.rebuild.core.service.dashboard.charts.FormatCalc;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据列表数据构建
 *
 * @author Zhao Fangfang
 * @since 1.0, 2019-6-20
 */
public class DataListBuilderImpl implements DataListBuilder {

    final protected Entity entity;
    final protected QueryParser queryParser;
    final protected ID user;

    /**
     * @param query
     * @param user
     */
    public DataListBuilderImpl(JSONObject query, ID user) {
        this.entity = MetadataHelper.getEntity(query.getString("entity"));
        this.queryParser = new QueryParser(query, this);
        this.user = user;
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    /**
     * @return
     */
    public QueryParser getQueryParser() {
        return queryParser;
    }

    @Override
    public String getDefaultFilter() {
        final int entity = queryParser.getEntity().getEntityCode();

        if (MetadataHelper.isBizzEntity(entity)) {
            List<String> where = new ArrayList<>();
            String s = UserFilters.getBizzFilter(entity, user);
            if (s != null) where.add(s);

            // 部门用户隔离
            s = UserFilters.getEnableBizzPartFilter(entity, user);
            if (s != null) where.add(s);
            return where.isEmpty() ? null : "( " + StringUtils.join(where, " and ") + " )";
        }

        return null;
    }

    /**
     * @return
     */
    protected boolean isNeedReload() {
        return queryParser.isNeedReload();
    }

    @Override
    public JSON getJSONResult() {
        long totalRows = 0;
        JSONArray stats = null;
        if (isNeedReload()) {
            List<Object[]> stats2 = getStats();
            totalRows = (long) stats2.get(0)[1];

            // 统计字段
            if (stats2.size() > 1) {
                stats = new JSONArray();
                for (int i = 1; i < stats2.size(); i++) {
                    stats.add(JSONUtils.toJSONObject(
                            new String[]{"label", "value", "color"}, stats2.get(i)));
                }
            }
        }

        Query query = Application.createQuery(queryParser.toSql(), user);
        int[] limits = queryParser.getSqlLimit();
        Object[][] data = query.setLimit(limits[0], limits[1]).array();

        JSONObject listdata = (JSONObject) createDataListWrapper((int) totalRows, data, query).toJson();
        if (stats != null) listdata.put("stats", stats);
        return listdata;
    }

    /**
     * 获取统计字段值
     *
     * @return
     */
    public JSONArray getJSONStats() {
        List<Object[]> stats2 = getStats();
        if (stats2.size() < 2) return null;

        JSONArray stats = new JSONArray();
        for (int i = 1; i < stats2.size(); i++) {
            stats.add(JSONUtils.toJSONObject(
                    new String[] {"label", "value", "color"}, stats2.get(i)));
        }
        return stats;
    }

    /**
     * 获取统计字段值
     *
     * @return
     */
    protected List<Object[]> getStats() {
        List<Object[]> stats = new ArrayList<>();

        final Object[] count = Application.createQuery(queryParser.toCountSql(), user).unique();
        stats.add(new Object[]{null, count[0]});
        if (count.length < 2) return stats;

        List<Map<String, Object>> statsFields = queryParser.getCountFields();
        if (statsFields.isEmpty()) return stats;

        // 统计列
        for (int i = 1; i < count.length; i++) {
            Map<String, Object> c = statsFields.get(i);
            Field field = entity.getField((String) c.get("field"));
            String label = (String) c.get("label2");
            if (StringUtils.isBlank(label)) {
                String calc = (String) c.get("calc");
                label = String.format("%s (%s)", Language.L(field), FormatCalc.valueOf(calc).getLabel());
            }

            EasyField easyField = EasyMetaFactory.valueOf(field);
            Object value = count[i];
            if (ChartsHelper.isZero(value)) {
                value = ChartsHelper.VALUE_ZERO;
            } else if (field.getType() == FieldType.LONG) {
                value = ObjectUtils.toLong(value);
            } else {
                value = easyField.wrapValue(value);
            }

            // fix: 3.5.4
            if (FieldValueHelper.isUseDesensitized(easyField, user)) {
                value = FieldValueHelper.desensitized(easyField, value);
            }

            stats.add(new Object[]{label, value, c.get("color")});
        }
        return stats;
    }

    /**
     * @param totalRows
     * @param data
     * @param query
     * @return
     */
    protected DataListWrapper createDataListWrapper(int totalRows, Object[][] data, Query query) {
        DataListWrapper wrapper = new DataListWrapper(
                totalRows, data, query.getSelectItems(), query.getRootEntity());
        wrapper.setPrivilegesFilter(user, queryParser.getQueryJoinFields());
        return wrapper;
    }
}
