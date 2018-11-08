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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.persist4j.Entity;

/**
 * 高级查询解析器
 * 
 * @author devezhao
 * @since 09/29/2018
 */
public class AdvFilterParser {
	
	private static final Log LOG = LogFactory.getLog(AdvFilterParser.class);
	
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
		JSONObject values = filterExp.getJSONObject("values");
		String equation = StringUtils.defaultIfBlank(filterExp.getString("equation"), "OR");
		
		Map<Integer, String> indexItemSqls = new LinkedHashMap<>();
		int noIndex = 1;
		for (Object item : items) {
			JSONObject jo = (JSONObject) item;
			Integer index = jo.getInteger("index");
			if (index == null) {
				index = noIndex++;
			}
			String itemSql = parseItem(jo, values);
			if (itemSql != null) {
				indexItemSqls.put(index, itemSql);
			}
		}
		if (indexItemSqls.isEmpty()) {
			return null;
		}
		
		if ("OR".equalsIgnoreCase(equation)) {
			return "( " + StringUtils.join(indexItemSqls.values(), " or ") + " )";
		} else if ("AND".equalsIgnoreCase(equation)) {
			return "( " + StringUtils.join(indexItemSqls.values(), " and ") + " )";
		} else {
			// 高级表达式 eg. (1 AND 2) or (3 AND 4)
			String tokens[] = equation.toLowerCase().split(" ");
			List<String> itemSqls = new ArrayList<>();
			for (int i = 0; i < tokens.length; i++) {
				String token = tokens[i];
				if (StringUtils.isBlank(token)) {
					continue;
				}
				if (NumberUtils.isDigits(token)) {
					String itemSql = StringUtils.defaultIfBlank(indexItemSqls.get(Integer.valueOf(token)), "(9=9)");
					itemSqls.add(itemSql);
				} else if (token.equals("(") || token.equals(")") || token.equals("or") || token.equals("and")) {
					itemSqls.add(token);
				} else {
					LOG.warn("Ignore equation token : " + token);
				}
			}
			return StringUtils.join(itemSqls, " ");
		}
	}
	
	/**
	 * @param item
	 * @param values
	 * @return
	 */
	protected String parseItem(JSONObject item, JSONObject values) {
		String field = item.getString("field");
		if (!rootEntity.containsField(field)) {
			return null;
		}
		
//		Field metaField = rootEntity.getField(field);
//		if (EasyMeta.getDisplayType(metaField) == DisplayType.PICKLIST) {
//			field = '&' + field;
//		}
		
		String op = convOp(item.getString("op"));
		StringBuffer sb = new StringBuffer(field)
				.append(' ')
				.append(op)
				.append(' ');
		// is null / is not null
		if (op.contains("null")) {
			return sb.toString();
		}
		
		String value = item.getString("value");
		if (StringUtils.isBlank(value)) {
			LOG.warn("Invalid item of advfilter : " + item.toJSONString());
			return null;
		}
		
		// 占位 {1}
		if (value.matches("\\{\\d+\\}")) {
			if (values == null) {
				LOG.warn("Invalid item of advfilter : " + item.toJSONString());
				return null;
			}
			
			String valHold = value.replaceAll("[\\{\\}]", "");
			value = parseVal(values.get(valHold), op);
		} else {
			value = parseVal(value, op);
		}
		if (value == null) {
			LOG.warn("Invalid item of advfilter : " + item.toJSONString());
			return null;
		}
		
		// 区间
		boolean isBetween = op.equals("between");
		String value2 = isBetween ? parseVal(item.getString("value2"), op) : null;
		if (isBetween && value2 == null) {
			value2 = value;
		}
		
		// like / not like
		if (op.contains("like")) {
			value = '%' + value + '%';
		}
		
		if (op.equals("in") || op.equals("not in")) {
			sb.append(value);
		} else {
			sb.append(quoteVal(value));
		}
		
		if (isBetween) {
//			sb.insert(0, "(").append(" and ").append(quoteVal(value2)).append(")");
			sb.append(" and ").append(quoteVal(value2));
		}
		return sb.toString();
	}
	
	/**
	 * @param val
	 * @param op
	 * @return
	 */
	private String parseVal(Object val, String op) {
		String value = null;
		// IN
		if (val instanceof JSONArray) {
			Set<String> array = new HashSet<>();
			for (Object o : (JSONArray) val) {
				array.add(quoteVal(o.toString()));
			}
			
			if (array.isEmpty()) {
				return null;
			} else {
				value = "( " + StringUtils.join(array, ",") + " )";
			}
			
		} else {
			value = val.toString();
			if (StringUtils.isBlank(value)) {
				return null;
			}
			
			// 兼容 | 号分割
			if (op.equals("in") || op.equals("not in")) {
				Set<String> array = new HashSet<>();
				for (String v : value.split("\\|")) {
					array.add(quoteVal(v));
				}
				value = "( " + StringUtils.join(array, ",") + " )";
			}
		}
		return value;
	}
	
	/**
	 * @param v
	 * @return
	 */
	private String quoteVal(String v) {
		if (NumberUtils.isDigits(v)) {
			return v;
		} else if (StringUtils.isNotBlank(v)) {
			return String.format("'%s'", StringEscapeUtils.escapeSql(v));
		}
		return "''";
	}
	
	/**
	 * @param op
	 * @return
	 */
	protected String convOp(String op) {
		if ("EQ".equalsIgnoreCase(op)) return "=";
		else if ("NEQ".equalsIgnoreCase(op)) return "<>";
		else if ("GT".equalsIgnoreCase(op)) return ">";
		else if ("LT".equalsIgnoreCase(op)) return "<";
		else if ("GE".equalsIgnoreCase(op)) return ">=";
		else if ("LE".equalsIgnoreCase(op)) return "<=";
		else if ("NL".equalsIgnoreCase(op)) return "is null";
		else if ("NT".equalsIgnoreCase(op)) return "is not null";
		else if ("LK".equalsIgnoreCase(op)) return "like";
		else if ("NLK".equalsIgnoreCase(op)) return "not like";
		else if ("IN".equalsIgnoreCase(op)) return "in";
		else if ("NIN".equalsIgnoreCase(op)) return "not in";
		else if ("BW".equalsIgnoreCase(op)) return "between";
		else if ("BFD".equalsIgnoreCase(op)) return "$before_day(%d)";
		else if ("BFM".equalsIgnoreCase(op)) return "$before_month(%d)";
		else if ("AFD".equalsIgnoreCase(op)) return "$after_day(%d)";
		else if ("AFM".equalsIgnoreCase(op)) return "$after_month(%d)";
		throw new UnsupportedOperationException("Unsupported token [" + op + "]");
	}
}
