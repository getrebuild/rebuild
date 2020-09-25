/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.SetUser;

import java.util.HashSet;
import java.util.Set;

/**
 * 批量选择记录（在列表中）查询器
 *
 * @author ZHAO
 * @since 2019/12/1
 */
public class BatchOperatorQuery extends SetUser<BatchOperatorQuery> {

    /**
     * 选中数据
     */
    public static final int DR_SELECTED = 1;
    /**
     * 当前页数据
     */
    public static final int DR_PAGED = 2;
    /**
     * 查询后数据
     */
    public static final int DR_QUERYED = 3;
    /**
     * 全部数据
     */
    public static final int DR_ALL = 10;

    private int dataRange;
    private JSONObject queryData;

    /**
     * @param dataRange
     * @param queryData
     */
    public BatchOperatorQuery(int dataRange, JSONObject queryData) {
        this.dataRange = dataRange;
        this.queryData = queryData;
    }

    /**
     * 对查询数据进行包装（根据数据范围）
     *
     * @param maxRows
     * @param clearFields
     * @return
     */
    public JSONObject wrapQueryData(int maxRows, boolean clearFields) {
        if (this.dataRange != DR_PAGED) {
            queryData.put("pageNo", 1);
            queryData.put("pageSize", maxRows);  // Max
        }
        if (this.dataRange == DR_SELECTED || this.dataRange == DR_ALL) {
            queryData.remove("filter");
            queryData.remove("advFilter");
        }
        if (this.dataRange == DR_SELECTED) {
            JSONObject idsItem = new JSONObject();
            idsItem.put("op", ParseHelper.IN);
            idsItem.put("field", getEntity().getPrimaryField().getName());
            idsItem.put("value", queryData.getString("_selected"));

            JSONArray items = new JSONArray();
            items.add(idsItem);
            JSONObject filter = new JSONObject();
            filter.put("items", items);
            queryData.put("filter", filter);
        }

        if (clearFields) {
            queryData.put("fields", new String[]{getEntity().getPrimaryField().getName()});
        }
        queryData.put("reload", Boolean.FALSE);
        return queryData;
    }

    /**
     * 获取 SQL from 后面的子句（含 from）
     *
     * @return
     * @see QueryParser
     */
    protected String getFromClauseSql() {
        QueryParser queryParser = new QueryParser(wrapQueryData(Integer.MAX_VALUE, true));
        String fullSql = queryParser.toSql();
        return fullSql.substring(fullSql.indexOf(" from ")).trim();
    }

    /**
     * 直接获取记录 ID[]
     *
     * @return
     */
    public ID[] getQueryedRecords() {
        if (this.dataRange == DR_SELECTED) {
            String selected = queryData.getString("_selected");

            Set<ID> ids = new HashSet<>();
            for (String s : selected.split("\\|")) {
                if (ID.isId(s)) {
                    ids.add(ID.valueOf(s));
                }
            }
            return ids.toArray(new ID[0]);
        }

        String sql = String.format("select %s %s",
                getEntity().getPrimaryField().getName(), getFromClauseSql());
        int pageNo = queryData.getIntValue("pageNo");
        int pageSize = queryData.getIntValue("pageSize");

        Object[][] array = Application.getQueryFactory().createQuery(sql, getUser())
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .setTimeout(60)
                .array();

        Set<ID> ids = new HashSet<>();
        for (Object[] o : array) {
            ids.add((ID) o[0]);
        }
        return ids.toArray(new ID[0]);
    }

    private Entity getEntity() {
        String entityName = queryData.getString("entity");
        return MetadataHelper.getEntity(entityName);
    }
}
