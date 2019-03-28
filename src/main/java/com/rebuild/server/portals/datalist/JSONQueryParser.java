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

package com.rebuild.server.portals.datalist;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.portals.AdvFilterManager;
import com.rebuild.server.service.query.AdvFilterParser;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 列表查询解析
 * 
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-20
 */
public class JSONQueryParser {

	protected JSONObject queryExpressie;
	private DataList dataListControl;
	
	private Entity entity;
	private List<Field> fieldList = new ArrayList<>();
	
	private String sql;
	private String countSql;
	private int[] limit;
	private boolean reload;
	
	/**
	 * @param queryExpressie
	 */
	public JSONQueryParser(JSONObject queryExpressie) {
		this(queryExpressie, null);
	}
	
	/**
	 * @param queryExpressie
	 * @param dataListControl
	 */
	public JSONQueryParser(JSONObject queryExpressie, DataList dataListControl) {
		this.queryExpressie = queryExpressie;
		this.dataListControl = dataListControl;
		
		this.entity = dataListControl != null ? 
				dataListControl.getEntity() : MetadataHelper.getEntity(queryExpressie.getString("entity"));
		
		JSONArray fieldsNode = queryExpressie.getJSONArray("fields");
		for (Object o : fieldsNode) {
			String field = o.toString().trim();
			fieldList.add(entity.getField(field));
		}
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
	protected Field[] getFieldList() {
		return fieldList.toArray(new Field[fieldList.size()]);
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
	public int[] getSqlLimit() {
		doParseIfNeed();
		return limit;
	}
	
	/**
	 * @return
	 */
	public boolean isNeedReload() {
		doParseIfNeed();
		return reload;
	}
	
	/**
	 * 解析 SQL
	 */
	protected void doParseIfNeed() {
		if (sql != null) {
			return;
		}
		
		StringBuffer sqlBase = new StringBuffer("select ");
		for (Field field : fieldList) {
			if (EasyMeta.getDisplayType(field) == DisplayType.PICKLIST) {  // TODO CLASSIFICATION
				sqlBase.append('&');
			}
			sqlBase.append(field.getName()).append(',');
		}
		// 最后增加一个主键列
		String pkName = entity.getPrimaryField().getName();
		sqlBase.append(pkName)
				.append(" from ")
				.append(entity.getName());
		
		// 过滤器
		
		StringBuffer sqlWhere = new StringBuffer(" where (1=1)");
		
		// Default
		String defaultFilter = dataListControl == null ? null : dataListControl.getDefaultFilter();
		if (defaultFilter != null) {
			sqlWhere.append(" and (").append(defaultFilter).append(')');
		}
		// Adv
		String advExpId = queryExpressie.getString("advFilter");
		if (ID.isId(advExpId)) {
			Object[] adv = AdvFilterManager.getAdvFilter(ID.valueOf(advExpId));
			if (adv != null) {
				String query = new AdvFilterParser(entity, (JSONObject) adv[1]).toSqlWhere();
				if (StringUtils.isNotBlank(query)) {
					sqlWhere.append(" and ").append(query);
				}
			}
		}
		// Quick
		JSONObject quickExp = queryExpressie.getJSONObject("filter");
		if (quickExp != null) {
			String query = new AdvFilterParser(entity, quickExp).toSqlWhere();
			if (StringUtils.isNotBlank(query)) {
				sqlWhere.append(" and ").append(query);
			}
		}
		sqlBase.append(sqlWhere);
		
		// 排序
		
		StringBuffer sqlSort = new StringBuffer(" order by ");
		
		String sortNode = queryExpressie.getString("sort");
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
		this.countSql = new StringBuffer("select ")
				.append("count(").append(pkName).append(')')
				.append(" from ")
				.append(entity.getName())
				.append(sqlWhere)
				.toString();
		
		int pageNo = NumberUtils.toInt(queryExpressie.getString("pageNo"), 1);
		int pageSize = NumberUtils.toInt(queryExpressie.getString("pageSize"), 20);
		this.limit = new int[] { pageSize, pageNo * pageSize - pageSize };
		this.reload = limit[1] == 0;
		if (!reload) {
			reload = BooleanUtils.toBoolean(queryExpressie.getString("reload"));
		}
	}
	
	/**
	 * @param sortNode
	 * @return
	 */
	private String parseSort(String sort) {
		String[] sort_s = sort.split(":");
		String sortField = sort_s[0];
		return sortField + ("desc".equalsIgnoreCase(sort_s[1]) ? " desc" : " asc");
	}
}
