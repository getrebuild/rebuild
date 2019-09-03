/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper.datalist;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.portals.AdvFilterManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.query.AdvFilterParser;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.util.HashMap;
import java.util.HashSet;
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
	private DataList dataListControl;
	
	private Entity entity;
	
	private String sql;
	private String countSql;
	private int[] limit;
	private boolean reload;
	
	private Map<String, Integer> queryJoinFields;
	
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
	public QueryParser(JSONObject queryExpr, DataList dataListControl) {
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
	public String toCountSql() {
		doParseIfNeed();
		return countSql;
	}
	
	/**
	 * @return
	 */
	protected Entity getEntity() {
		return entity;
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
		
		StringBuilder sqlBase = new StringBuilder("select ");
		
		JSONArray fieldsNode = queryExpr.getJSONArray("fields");
		int fieldIndex = -1;
		Set<String> queryJoinFields = new HashSet<>();
		for (Object o : fieldsNode) {
			// 在 DataListManager 中已验证字段有效，此处不再次验证
			String field = o.toString().trim();
			sqlBase.append(field).append(',');
			fieldIndex++;
			
			if (field.split("\\.").length > 1) {
				queryJoinFields.add(field.split("\\.")[0]);
			}
		}
		
		// 最后增加一个主键列
		String pkName = entity.getPrimaryField().getName();
		sqlBase.append(pkName);
		fieldIndex++;
		
		// 查询关联项 ID 以验证权限
		if (!queryJoinFields.isEmpty()) {
			this.queryJoinFields = new HashMap<>();
			for (String field : queryJoinFields) {
				sqlBase.append(',').append(field);
				fieldIndex++;
				this.queryJoinFields.put(field, fieldIndex);
			}
		}
		
		sqlBase.append(" from ").append(entity.getName());
		
		// 过滤器
		
		StringBuilder sqlWhere = new StringBuilder(" where (1=1)");
		
		// Default
		String defaultFilter = dataListControl == null ? null : dataListControl.getDefaultFilter();
		if (defaultFilter != null) {
			sqlWhere.append(" and (").append(defaultFilter).append(')');
		}
		// Adv
		String advExpId = queryExpr.getString("advFilter");
		if (ID.isId(advExpId)) {
			ConfigEntry adv = AdvFilterManager.instance.getAdvFilter(ID.valueOf(advExpId));
			if (adv != null) {
				String where = new AdvFilterParser(entity, (JSONObject) adv.getJSON("filter")).toSqlWhere();
				if (StringUtils.isNotBlank(where)) {
					sqlWhere.append(" and ").append(where);
				}
			}
		}
		// Quick
		JSONObject quickExp = queryExpr.getJSONObject("filter");
		if (quickExp != null) {
			String where = new AdvFilterParser(entity, quickExp).toSqlWhere();
			if (StringUtils.isNotBlank(where)) {
				sqlWhere.append(" and ").append(where);
			}
		}
		sqlBase.append(sqlWhere);
		
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
			sqlBase.append(sqlSort);
		}
		
		this.sql = sqlBase.toString();
		this.countSql = new StringBuilder("select ")
				.append("count(").append(pkName).append(')')
				.append(" from ")
				.append(entity.getName())
				.append(sqlWhere)
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
}
