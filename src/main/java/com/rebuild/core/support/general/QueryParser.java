/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.AdvFilterManager;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.query.ParseHelper;
import lombok.Getter;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 列表查询解析
 *
 * @author Zhao Fangfang
 * @since 1.0, 2018-6-20
 */
public class QueryParser {

    private JSONObject queryExpr;
    private DataListBuilder dataListBuilder;

    @Getter
    private Entity entity;

    private String sql;
    private String countSql;
    private List<Map<String, Object>> countFields;
    private int[] limit;
    private boolean reload;

    // 连接字段（跨实体查询的字段）
    private Map<String, Integer> queryJoinFields;

    // 查询字段
    private List<String> queryFields = new ArrayList<>();

    /**
     * @param queryExpr
     */
    public QueryParser(JSONObject queryExpr) {
        this(queryExpr, null);
    }

    /**
     * @param queryExpr
     * @param dataListBuilder
     */
    protected QueryParser(JSONObject queryExpr, DataListBuilder dataListBuilder) {
        this.queryExpr = queryExpr;
        this.dataListBuilder = dataListBuilder;
        this.entity = dataListBuilder != null ?
                dataListBuilder.getEntity() : MetadataHelper.getEntity(queryExpr.getString("entity"));
    }

    /**
     * @return
     */
    public String toSql() {
        doParseIfNeed();
        return sql;
    }

    /**
     * @return
     */
    protected String toCountSql() {
        doParseIfNeed();
        return countSql;
    }

    /**
     * 获取查询字段
     *
     * @return
     */
    public List<String> getQueryFields() {
        doParseIfNeed();
        return queryFields;
    }

    /**
     * @return
     */
    public int[] getSqlLimit() {
        doParseIfNeed();
        return limit;
    }

    /**
     * @return
     */
    protected boolean isNeedReload() {
        doParseIfNeed();
        return reload;
    }

    /**
     * @return
     */
    public Map<String, Integer> getQueryJoinFields() {
        doParseIfNeed();
        return queryJoinFields;
    }

    /**
     * @return
     */
    protected List<Map<String, Object>> getCountFields() {
        doParseIfNeed();
        return countFields;
    }

    /**
     * 解析 SQL
     */
    private void doParseIfNeed() {
        if (sql != null) return;

        StringBuilder fullSql = new StringBuilder("select ");

        JSONArray fieldsNode = queryExpr.getJSONArray("fields");
        int fieldIndex = -1;
        Set<String> queryJoinFields = new HashSet<>();
        for (Object o : fieldsNode) {
            // 在 DataListManager 中已验证字段有效，此处不再次验证
            String field = o.toString().trim();
            fullSql.append(field).append(',');
            fieldIndex++;

            // 仅支持两级验证
            if (field.split("\\.").length > 1) {
                queryJoinFields.add(field.split("\\.")[0]);
            }

            this.queryFields.add(field);
        }

        // 最后增加一个主键列
        String pkName = entity.getPrimaryField().getName();
        fullSql.append(pkName);
        fieldIndex++;

        // 追加关联查询记录 ID 以便验证权限
        if (!queryJoinFields.isEmpty()) {
            this.queryJoinFields = new HashMap<>();
            for (String field : queryJoinFields) {
                fullSql.append(',').append(field);
                fieldIndex++;
                this.queryJoinFields.put(field, fieldIndex);
            }
        }

        fullSql.append(" from ").append(entity.getName());

        // 过滤器

        List<String> wheres = new ArrayList<>();

        // Default
        String defaultFilter = dataListBuilder == null ? null : dataListBuilder.getDefaultFilter();
        if (StringUtils.isNotBlank(defaultFilter)) {
            wheres.add(defaultFilter);
        }

        // append: ProtocolFilter
        String protocolFilter = queryExpr.getString("protocolFilter");
        if (StringUtils.isNotBlank(protocolFilter)) {
            String where = new ProtocolFilterParser(protocolFilter).toSqlWhere();
            if (StringUtils.isNotBlank(where)) wheres.add(where);
        }

        // append: AdvFilter
        String advFilter = queryExpr.getString("advFilter");
        if (ID.isId(advFilter)) {
            String where = parseAdvFilter(ID.valueOf(advFilter));
            if (StringUtils.isNotBlank(where)) wheres.add(where);
        }

        // append: QuickQuery
        JSONObject quickFilter = queryExpr.getJSONObject("filter");
        if (quickFilter != null) {
            String where = new AdvFilterParser(quickFilter, entity).toSqlWhere();
            if (StringUtils.isNotBlank(where)) wheres.add(where);
        }
        // v3.3
        JSONObject quickFilterAnd = queryExpr.getJSONObject("filterAnd");
        if (quickFilterAnd != null) {
            String where = new AdvFilterParser(quickFilterAnd, entity).toSqlWhere();
            if (StringUtils.isNotBlank(where)) wheres.add(where);
        }

        final String sqlWhere = wheres.isEmpty() ? "1=1" : StringUtils.join(wheres.iterator(), " and ");
        fullSql.append(" where ").append(sqlWhere);

        // 排序

        String sortNode = queryExpr.getString("sort");
        String sortSql = null;
        if (StringUtils.isNotBlank(sortNode)) {
            sortSql = parseSort(sortNode);
        } else if (entity.containsField(EntityHelper.ModifiedOn)) {
            sortSql = EntityHelper.ModifiedOn + " desc";
        } else if (entity.containsField(EntityHelper.CreatedOn)) {
            sortSql = EntityHelper.CreatedOn + " desc";
        }
        if (StringUtils.isNotBlank(sortSql)) fullSql.append(" order by ").append(sortSql);

        this.sql = fullSql.toString();
        this.countSql = this.buildCountSql(pkName) + sqlWhere;

        int pageNo = NumberUtils.toInt(queryExpr.getString("pageNo"), 1);
        int pageSize = NumberUtils.toInt(queryExpr.getString("pageSize"), 40);
        this.limit = new int[] { pageSize, pageNo * pageSize - pageSize };

        this.reload = limit[1] == 0;
        if (!reload) {
            reload = BooleanUtils.toBoolean(queryExpr.getString("reload"));
        }
    }

    /**
     * 排序字段（支持多个）
     *
     * @param sort
     * @return
     */
    private String parseSort(String sort) {
        if (sort.length() < 5) return null;

        StringBuilder sb = new StringBuilder();
        String[] sorts = sort.split("[,;]");
        for (String s : sorts) {
            String[] split = s.split(":");
            if (StringUtils.isBlank(split[0])) return null;

            sb.append(split[0]);
            if (split.length > 1) sb.append("desc".equalsIgnoreCase(split[1]) ? " desc" : " asc");
            sb.append(", ");
        }

        // Remove last ` ,`
        int len = sb.length();
        sb.delete(len - 2, len);
        return sb.toString();
    }

    /**
     * @param filterId
     * @return
     */
    private String parseAdvFilter(ID filterId) {
        ConfigBean advFilter = AdvFilterManager.instance.getAdvFilter(filterId);
        if (advFilter != null) {
            JSONObject filterExpr = (JSONObject) advFilter.getJSON("filter");
            if (ParseHelper.validAdvFilter(filterExpr)) {
                return new AdvFilterParser(filterExpr, entity).toSqlWhere();
            }
        }
        return null;
    }

    /**
     * @param pkName
     * @return
     */
    private String buildCountSql(String pkName) {
        List<String> counts = new ArrayList<>();
        counts.add(String.format("count(%s)", pkName));

        countFields = new ArrayList<>();
        countFields.add(Collections.emptyMap());

        if (queryExpr.getBooleanValue("statsField")) {
            ConfigBean cb = DataListManager.instance.getListStats(UserService.SYSTEM_USER, entity.getName());
            if (cb != null && cb.getJSON("config") != null) {
                JSONArray items = ((JSONObject) cb.getJSON("config")).getJSONArray("items");
                for (Object o : items) {
                    JSONObject item = (JSONObject) o;
                    String field = item.getString("field");
                    if (MetadataHelper.checkAndWarnField(entity, field)) {
                        counts.add(String.format("%s(%s)", item.getString("calc"), field));
                        countFields.add(item);
                    }
                }
            }
        }

        return String.format("select %s from %s where ",
                StringUtils.join(counts, ","), entity.getName());
    }
}
