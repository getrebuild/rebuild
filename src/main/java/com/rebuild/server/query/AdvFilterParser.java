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

package com.rebuild.server.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.entityhub.DisplayType;
import com.rebuild.server.entityhub.EasyMeta;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;

/**
 * 高级查询解析器
 * 
 * @author devezhao
 * @since 09/29/2018
 */
public class AdvFilterParser {
	
	private Entity rootEntity;
	private JSONObject filterExp;
	
	/**
	 * @param filterExp
	 */
	public AdvFilterParser(JSONObject filterExp) {
		String entity = filterExp.getString("entity");
		Assert.notNull(entity, "[entity] node can't be blank");
		this.rootEntity = MetadataHelper.getEntity(entity);
		this.filterExp = filterExp;
	}
	
	/**
	 * @param rootEntity
	 * @param filterExp
	 */
	public AdvFilterParser(Entity rootEntity, JSONObject filterExp) {
		this.rootEntity = rootEntity;
		this.filterExp = filterExp;
	}
	
	public String toSqlWhere() {
		JSONArray items = filterExp.getJSONArray("items");
		JSONObject qvalues = filterExp.getJSONObject("values");
		String equation = StringUtils.defaultIfBlank(filterExp.getString("equation"), "OR");
		
		List<String> itemsSql = new ArrayList<>();
		for (Object item : items) {
			String itemSql = parseItem((JSONObject) item, qvalues);
			if (itemSql != null) {
				itemsSql.add(itemSql);
			}
		}
		if (itemsSql.isEmpty()) {
			return null;
		}
		
		if ("OR".equalsIgnoreCase(equation)) {
			return "( " + StringUtils.join(itemsSql, " or ") + " )";
		} else if ("AND".equalsIgnoreCase(equation)) {
			return "( " + StringUtils.join(itemsSql, " and ") + " )";
		}
		
		// TODO 高级表达式
		
		return null;
	}
	
	/**
	 * @param item
	 * @param qvalues
	 * @return
	 */
	protected String parseItem(JSONObject item, JSONObject qvalues) {
		String field = item.getString("field");
		if (!rootEntity.containsField(field)) {
			return null;
		}
		
		Field metaField = rootEntity.getField(field);
		if (EasyMeta.getDisplayType(metaField) == DisplayType.PICKLIST) {
			field = '&' + field;
		}
		
		String op = convOp(item.getString("op"));
		
		String value = item.getString("value");
		if (value.matches("\\{\\d+\\}")) {
			String valIndex = value.replaceAll("[\\{\\}]", "");
			Object valHold = qvalues.get(valIndex);
			if (valHold == null) {
				return null;
			}
			
			// in
			if (valHold instanceof JSONArray) {
				Set<String> valArray = new HashSet<>();
				for (Object o : (JSONArray) valHold) {
					valArray.add(quote(o.toString()));
				}
				
				if (valArray.isEmpty()) {
					return null;
				} else {
					value = "( " + StringUtils.join(valArray, ", ") + " )";
				}
				
			} else {
				value = valHold.toString();
				if (StringUtils.isBlank(value)) {
					return null;
				}
			}
		}
		
		if (op.contains("like")) {
			value = '%' + value + '%';
		}

		StringBuffer sb = new StringBuffer(field)
				.append(' ')
				.append(op)
				.append(' ');
		
		if ("in".equals(op)) {
			sb.append(value);
		} else if (NumberUtils.isDigits(value)) {
			sb.append(value);
		} else if (StringUtils.isNotBlank(value)) {
			sb.append(quote(value));
		}
		return sb.toString();
	}
	
	private String quote(String v) {
		return String.format("'%s'", StringEscapeUtils.escapeSql(v));
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
		if ("in".equals(op)) return "in";
		throw new UnsupportedOperationException("Unsupported token [" + op + "]");
	}
}
