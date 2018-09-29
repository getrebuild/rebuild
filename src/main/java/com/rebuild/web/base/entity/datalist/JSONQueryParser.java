/*
rebuild - Building your system freely.
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

package com.rebuild.web.base.entity.datalist;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.entityhub.DisplayType;
import com.rebuild.server.entityhub.EasyMeta;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;

/**
 * 列表查询解析
 * 
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-20
 */
public class JSONQueryParser {

	protected JSONObject queryElement;
	
	private DataListControl dataListControl;
	
	private Entity entity;
	private List<Field> fieldList = new ArrayList<>();
	
	private String sql;
	private String countSql;
	private int[] limit;
	private boolean reload;
	
	/**
	 * @param queryElement
	 * @param dataListControl
	 */
	public JSONQueryParser(JSONObject queryElement, DataListControl dataListControl) {
		this.queryElement = queryElement;
		this.dataListControl = dataListControl;
		
		this.entity = MetadataHelper.getEntity(queryElement.getString("entity"));
		
		JSONArray fieldsNode = queryElement.getJSONArray("fields");
		for (Object o : fieldsNode) {
			String field = o.toString().trim();
			fieldList.add(entity.getField(field));
		}
	}
	
	public Entity getEntity() {
		return entity;
	}
	
	public Field[] getFieldList() {
		return fieldList.toArray(new Field[fieldList.size()]);
	}
	
	public String toSql() {
		doParseIfNeed();
		return sql;
	}
	
	public String toSqlCount() {
		doParseIfNeed();
		return countSql;
	}
	
	public int[] getSqlLimit() {
		doParseIfNeed();
		return limit;
	}
	
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
			if (EasyMeta.getDisplayType(field) == DisplayType.PICKLIST) {
				sqlBase.append('&');
			}
			sqlBase.append(field.getName()).append(',');
		}
		// 最后增加一个主键列
		String pkName = entity.getPrimaryField().getName();
		sqlBase.append(pkName)
				.append(" from ")
				.append(entity.getName());
		
		StringBuffer sqlWhere = new StringBuffer(" where (1=1)");
		if (dataListControl.getDefaultFilter() != null) {
			sqlWhere.append('(').append(dataListControl.getDefaultFilter()).append(')');
		}
		
		JSONArray filterNode = queryElement.getJSONArray("filter");
		if (filterNode != null && !filterNode.isEmpty()) {
			sqlWhere.append(parseFilter(filterNode));
		}
		sqlBase.append(sqlWhere);
		
		StringBuffer sqlSort = new StringBuffer(" order by ");
		if (dataListControl.getDefaultFilter() != null) {
			sqlWhere.append('(').append(dataListControl.getDefaultFilter()).append(')');
		}
		
		String sortNode = queryElement.getString("sort");
		if (StringUtils.isNotBlank(sortNode)) {
			sqlSort.append(parseSort(sortNode));
		} else if (entity.containsField(EntityHelper.modifiedOn)) {
			sqlSort.append(EntityHelper.modifiedOn + " desc");
		} else if (entity.containsField(EntityHelper.createdOn)) {
			sqlSort.append(EntityHelper.createdOn + " desc");
		}
		sqlBase.append(sqlSort);
		
		this.sql = sqlBase.toString();
		this.countSql = new StringBuffer("select ")
				.append("count(").append(pkName).append(')')
				.append(" from ")
				.append(entity.getName())
				.append(sqlWhere)
				.toString();
		
		int pageNo = NumberUtils.toInt(queryElement.getString("pageNo"), 1);
		int pageSize = NumberUtils.toInt(queryElement.getString("pageSize"), 20);
		this.limit = new int[] { pageSize, pageNo * pageSize - pageSize };
		this.reload = limit[1] == 0;
		if (!reload) {
			reload = BooleanUtils.toBoolean(queryElement.getString("reload"));
		}
	}
	
	/**
	 * @param filterNode
	 * @return
	 */
	protected String parseFilter(JSONArray filterNode) {
		StringBuffer sb = new StringBuffer();
		for (Object o : filterNode) {
			JSONObject el = (JSONObject) o;
			String field = el.getString("field");
			String value = el.getString("value");
			String op = el.getString("op");
			op = convOp(op);
			
			if ("in".equals(op) || "exists".equals(op)) {
				throw new UnsupportedOperationException("Unsupported 'in' or 'exists'");
			} else {
				sb.append(" and ")
						.append(field)
						.append(' ')
						.append(op)
						.append(' ');
				if (NumberUtils.isDigits(value)) {
					sb.append(value);
				} else if (StringUtils.isNotBlank(value)) {
					sb.append('\'').append(StringEscapeUtils.escapeSql(value)).append('\'');
				}
			}
		}
		return sb.toString();
	}
	
	/**
	 * @param sortNode
	 * @return
	 */
	protected String parseSort(String sort) {
		String[] sort_s = sort.split(":");
		String sortField = sort_s[0];
		return sortField + ("desc".equalsIgnoreCase(sort_s[1]) ? " desc" : " asc");
	}
	
	/**
	 * @param op
	 * @return
	 */
	protected String convOp(String op) {
		if ("eq".equals(op)) return "=";
		if ("neq".equals(op)) return "<>";
		if ("gt".equals(op)) return ">";
		if ("lt".equals(op)) return "<";
		if ("ge".equals(op)) return ">=";
		if ("le".equals(op)) return "<=";
		if ("nl".equals(op)) return "is null";
		if ("nt".equals(op)) return "is not null";
		if ("lk".equals(op)) return "like";
		if ("nlk".equals(op)) return "not like";
		if ("gb".equals(op)) return "group by";
		return op;
	}
}
