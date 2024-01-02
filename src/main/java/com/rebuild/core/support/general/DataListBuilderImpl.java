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
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserFilters;
import com.rebuild.core.privileges.UserService;
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
 * @since 1.0, 2018-6-20
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

        if (entity == EntityHelper.User || entity == EntityHelper.Department || entity == EntityHelper.Team || entity == EntityHelper.Role) {
            List<String> where = new ArrayList<>();
            // 隐藏系统用户
            if (entity == EntityHelper.User) {
                where.add(String.format("userId <> '%s'", UserService.SYSTEM_USER));
            }

            // 部门用户隔离
            String s = UserFilters.getEnableBizzPartFilter(entity, this.user);
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
        int totalRows = 0;
        List<JSON> stats = null;
        if (isNeedReload()) {
            Object[] count = Application.createQuery(queryParser.toCountSql(), user).unique();
            totalRows = ObjectUtils.toInt(count[0]);

            // 统计列
            List<Map<String, Object>> countFields = queryParser.getCountFields();
            if (count.length > 1 && !countFields.isEmpty()) {
                stats = new ArrayList<>();
                for (int i = 1; i < count.length; i++) {
                    Map<String, Object> cfg = countFields.get(i);
                    Field field = entity.getField((String) cfg.get("field"));
                    String label = (String) cfg.get("label2");
                    if (StringUtils.isBlank(label)) {
                        String calc = (String) cfg.get("calc");
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
                    if (FieldValueHelper.isUseDesensitized(easyField, this.user)) {
                        value = FieldValueHelper.desensitized(easyField, value);
                    }

                    stats.add(JSONUtils.toJSONObject(new String[] { "label", "value" }, new Object[] {label,value} ));
                }
            }
        }

        Query query = Application.createQuery(queryParser.toSql(), user);
        int[] limits = queryParser.getSqlLimit();
        Object[][] data = query.setLimit(limits[0], limits[1]).array();

        JSONObject listdata = (JSONObject) createDataListWrapper(totalRows, data, query).toJson();
        if (stats != null) listdata.put("stats", stats);
        return listdata;
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
