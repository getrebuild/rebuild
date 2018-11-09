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

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;

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
				indexItemSqls.put(index, itemSql.trim());
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
		
		final Field fieldMeta = rootEntity.getField(field);
		final String op = item.getString("op");
		
		String oper = convOp(op);
		StringBuffer sb = new StringBuffer(field)
				.append(' ')
				.append(oper)
				.append(' ');
		// is null / is not null
		if (oper.contains("null")) {
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
			value = parseValue(values.get(valHold), oper, fieldMeta);
		} else {
			value = parseValue(value, oper, fieldMeta);
		}
		if (value == null) {
			LOG.warn("Invalid item of advfilter : " + item.toJSONString());
			return null;
		}
		
		// TODO 自定义函数
		if ("BFD".equalsIgnoreCase(op)) {
			value = CalendarUtils.getUTCDateFormat().format(CalendarUtils.addDay(-NumberUtils.toInt(value)));
		} else if ("AFD".equalsIgnoreCase(op)) {
			value = CalendarUtils.getUTCDateFormat().format(CalendarUtils.addDay(NumberUtils.toInt(value)));
		} else if ("BFM".equalsIgnoreCase(op)) {
			value = CalendarUtils.getUTCDateFormat().format(CalendarUtils.addMonth(-NumberUtils.toInt(value)));
		} else if ("AFM".equalsIgnoreCase(op)) {
			value = CalendarUtils.getUTCDateFormat().format(CalendarUtils.addMonth(NumberUtils.toInt(value)));
		}
		
		// 区间
		boolean isBetween = oper.equals("between");
		String value2 = isBetween ? parseValue(item.getString("value2"), oper, fieldMeta) : null;
		if (isBetween && value2 == null) {
			value2 = value;
		}
		
		if (oper.equals("in") || oper.equals("not in")) {
			sb.append(value);
		} else {
			// like / not like
			if (oper.contains("like")) {
				value = '%' + value + '%';
			}
			sb.append(quoteValue(value, fieldMeta.getType()));
		}
		
		if (isBetween) {
			sb.append(" and ").append(quoteValue(value2, fieldMeta.getType()));
		}
		return sb.toString();
	}
	
	/**
	 * @param val
	 * @param op
	 * @param type
	 * @return
	 */
	private String parseValue(Object val, String op, Field field) {
		String value = null;
		// IN
		if (val instanceof JSONArray) {
			Set<String> inVals = new HashSet<>();
			for (Object v : (JSONArray) val) {
				inVals.add(quoteValue(v.toString(), field.getType()));
			}
			return optimizeIn(inVals);
			
		} else {
			value = val.toString();
			if (StringUtils.isBlank(value)) {
				return null;
			}
			
			// 兼容 | 号分割
			if (op.equals("in") || op.equals("not in")) {
				Set<String> inVals = new HashSet<>();
				for (String v : value.split("\\|")) {
					inVals.add(quoteValue(v, field.getType()));
				}
				return optimizeIn(inVals);
			}
		}
		return value;
	}
	
	/**
	 * TODO 优化 in 为 OR
	 * 
	 * @param inVals
	 * @return
	 */
	private String optimizeIn(Set<String> inVals) {
		if (inVals == null || inVals.isEmpty()) {
			return null;
		}
		return "( " + StringUtils.join(inVals, ",") + " )";
	}
	
	/**
	 * @param val
	 * @param type
	 * @return
	 */
	private String quoteValue(String val, Type type) {
		if (NumberUtils.isNumber(val) && isNumberType(type)) {
			return val;
		} else if (StringUtils.isNotBlank(val)) {
			return String.format("'%s'", StringEscapeUtils.escapeSql(val));
		}
		return "''";
	}
	
	/**
	 * @param op
	 * @return
	 */
	private String convOp(String op) {
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
		else if ("BFD".equalsIgnoreCase(op)) return "<"; //"$before_day(%d)";
		else if ("BFM".equalsIgnoreCase(op)) return "<"; //"$before_month(%d)";
		else if ("AFD".equalsIgnoreCase(op)) return ">"; //"$after_day(%d)";
		else if ("AFM".equalsIgnoreCase(op)) return ">"; //"$after_month(%d)";
		throw new UnsupportedOperationException("Unsupported token [" + op + "]");
	}
	
	/**
	 * @param type
	 * @return
	 */
	private boolean isNumberType(Type type) {
		return type == FieldType.INT || type == FieldType.SMALL_INT || type == FieldType.LONG 
				|| type == FieldType.DOUBLE || type == FieldType.DECIMAL;
	}
}
