/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.datalist;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.portals.AdvFilterManager;
import com.rebuild.server.configuration.portals.ChartManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.query.AdvFilterParser;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 列表查询解析
 * 
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-20
 */
public class QueryParser {
	
	private JSONObject queryExpr;
	private DataListControl dataListControl;
	
	private Entity entity;
	
	private String sql;
	private String countSql;
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
	 * @param dataListControl
	 */
	protected QueryParser(JSONObject queryExpr, DataListControl dataListControl) {
		this.queryExpr = queryExpr;
		this.dataListControl = dataListControl;
		this.entity = dataListControl != null ? 
				dataListControl.getEntity() : MetadataHelper.getEntity(queryExpr.getString("entity"));
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
	 * @return
	 */
	public Entity getEntity() {
		return entity;
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
	protected int[] getSqlLimit() {
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
	protected Map<String, Integer> getQueryJoinFields() {
		doParseIfNeed();
		return queryJoinFields;
	}

	/**
	 * 解析 SQL
	 */
	private void doParseIfNeed() {
		if (sql != null) {
			return;
		}
		
		StringBuilder fullSql = new StringBuilder("select ");
		
		JSONArray fieldsNode = queryExpr.getJSONArray("fields");
		int fieldIndex = -1;
		Set<String> queryJoinFields = new HashSet<>();
		for (Object o : fieldsNode) {
			// 在 DataListManager 中已验证字段有效，此处不再次验证
			String field = o.toString().trim();
			fullSql.append(field).append(',');
			fieldIndex++;
			
			if (field.split("\\.").length > 1) {
				queryJoinFields.add(field.split("\\.")[0]);
			}

			this.queryFields.add(field);
		}
		
		// 最后增加一个主键列
		String pkName = entity.getPrimaryField().getName();
		fullSql.append(pkName);
		fieldIndex++;

		// NOTE 查询出关联记录 ID 以便验证权限
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
		
		final StringBuilder sqlWhere = new StringBuilder("(1=1)");
		
		// Default
		String defaultFilter = dataListControl == null ? null : dataListControl.getDefaultFilter();
		if (StringUtils.isNotBlank(defaultFilter)) {
			sqlWhere.append(" and (").append(defaultFilter).append(')');
		}

		// appends AdvFilter
		String advFilter = queryExpr.getString("advFilter");
		if (ID.isId(advFilter)) {
            String where = parseAdvFilter(ID.valueOf(advFilter));
            if (StringUtils.isNotBlank(where)) {
                sqlWhere.append(" and ").append(where);
            }
		}

		// appends Quick
		JSONObject quickFilter = queryExpr.getJSONObject("filter");
		if (quickFilter != null) {
			String where = new AdvFilterParser(entity, quickFilter).toSqlWhere();
			if (StringUtils.isNotBlank(where)) {
				sqlWhere.append(" and ").append(where);
			}
		}
		fullSql.append(" where ").append(sqlWhere);
		
		// 排序
		
		StringBuilder sqlSort = new StringBuilder(" order by ");
		
		String sortNode = queryExpr.getString("sort");
		if (StringUtils.isNotBlank(sortNode)) {
			sqlSort.append(parseSort(sortNode));
		} else if (entity.containsField(EntityHelper.ModifiedOn)) {
			sqlSort.append(EntityHelper.ModifiedOn + " desc");
		} else if (entity.containsField(EntityHelper.CreatedOn)) {
			sqlSort.append(EntityHelper.CreatedOn + " desc");
		}
		if (sqlSort.length() > 10) {
			fullSql.append(sqlSort);
		}
		
		this.sql = fullSql.toString();
		this.countSql = new StringBuilder("select ")
				.append("count(").append(pkName).append(')')
				.append(" from ").append(entity.getName())
                .append(" where ").append(sqlWhere)
				.toString();

		int pageNo = NumberUtils.toInt(queryExpr.getString("pageNo"), 1);
		int pageSize = NumberUtils.toInt(queryExpr.getString("pageSize"), 20);
		this.limit = new int[] { pageSize, pageNo * pageSize - pageSize };
		this.reload = limit[1] == 0;
		if (!reload) {
			reload = BooleanUtils.toBoolean(queryExpr.getString("reload"));
		}
	}
	
	/**
	 * @param sort
	 * @return
	 */
	private String parseSort(String sort) {
		String[] sort_s = sort.split(":");
		String sortField = sort_s[0];
		return sortField + ("desc".equalsIgnoreCase(sort_s[1]) ? " desc" : " asc");
	}

    /**
     * @param filterId
     * @return
     */
	private String parseAdvFilter(ID filterId) {
	    // via Charts
	    if (filterId.getEntityCode() == EntityHelper.ChartConfig) {
            ConfigEntry chart = ChartManager.instance.getChart(filterId);
            JSONObject filterExp = ((JSONObject) chart.getJSON("config")).getJSONObject("filter");
            return new AdvFilterParser(entity, filterExp).toSqlWhere();
        }

	    // AdvFilter
        ConfigEntry advFilter = AdvFilterManager.instance.getAdvFilter(filterId);
        if (advFilter != null) {
            JSONObject filterExp = (JSONObject) advFilter.getJSON("filter");
            return new AdvFilterParser(entity, filterExp).toSqlWhere();
        }

        return null;
    }
}
