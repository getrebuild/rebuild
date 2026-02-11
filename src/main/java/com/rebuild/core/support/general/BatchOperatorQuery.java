/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.MediaValue;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.SetUser;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 批量选择记录（在列表中）查询器
 *
 * @author ZHAO
 * @since 2019/12/1
 */
public class BatchOperatorQuery extends SetUser {

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
            queryData.put("pageSize", maxRows);
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
        } else {
            // v4.3 空则全部
            if (CollectionUtils.isEmpty(queryData.getJSONArray("fields"))) {
                List<String> allFields = new ArrayList<>();
                for (Field field : MetadataSorter.sortFields(getEntity())) {
                    EasyField easyField = EasyMetaFactory.valueOf(field);
                    if (!easyField.getDisplayType().isExportable() || easyField instanceof MediaValue) continue;

                    allFields.add(field.getName());
                }
                queryData.put("fields", allFields.toArray(new String[0]));
            }
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
    public ID[] getQueryedRecordIds() {
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

        String sql = String.format("select %s %s", getEntity().getPrimaryField().getName(), getFromClauseSql());
        int pageNo = queryData.getIntValue("pageNo");
        int pageSize = queryData.getIntValue("pageSize");

        Object[][] array = Application.createQuery(sql, getUser())
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .setTimeout(180)
                .array();

        Set<ID> ids = new HashSet<>();
        for (Object[] o : array) {
            ids.add((ID) o[0]);
        }
        return ids.toArray(new ID[0]);
    }

    private Entity getEntity() {
        return MetadataHelper.getEntity(queryData.getString("entity"));
    }

    // --

    /**
     * @param request
     * @return
     */
    public static BatchOperatorQuery create(HttpServletRequest request, String entity) {
        JSONObject requestData = (JSONObject) ServletUtils.getRequestJson(request);
        requestData.put("entity", entity);

        int dr = NumberUtils.toInt(request.getParameter("dr"), DR_PAGED);
        return new BatchOperatorQuery(dr, requestData);
    }
}
