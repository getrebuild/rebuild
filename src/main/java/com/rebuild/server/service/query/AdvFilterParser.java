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

package com.rebuild.server.service.query;

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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.privileges.Department;
import com.rebuild.web.IllegalParameterException;

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
	
	private JSONObject filterExp;
	private Entity rootEntity;
	
	/**
	 * @param filterExp
	 */
	public AdvFilterParser(JSONObject filterExp) {
		this.rootEntity = MetadataHelper.getEntity(filterExp.getString("entity"));
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
	
	/**
	 * @return
	 */
	public String toSqlWhere() {
		// 快速过滤模式，自动确定查询项
		if ("QUICK".equalsIgnoreCase(filterExp.getString("type"))) {
			JSONArray items = buildQuickFilterItems(filterExp.getString("qfields"));
			this.filterExp.put("items", items);
		}
		
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
		
		if (validEquation(equation) == null) {
			throw new FilterParseException("无效高级表达式 : " + equation);
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
				
				boolean hasRP = false;  // the `)`
				if (token.length() > 1) {
					if (token.startsWith("(")) {
						itemSqls.add("(");
						token = token.substring(1);
					} else if (token.endsWith(")")) {
						hasRP = true;
						token = token.substring(0, token.length() - 1);
					}
				}
				
				if (NumberUtils.isDigits(token)) {
					String itemSql = StringUtils.defaultIfBlank(indexItemSqls.get(Integer.valueOf(token)), "(9=9)");
					itemSqls.add(itemSql);
				} else if (token.equals("(") || token.equals(")") || token.equals("or") || token.equals("and")) {
					itemSqls.add(token);
				} else {
					LOG.warn("Invalid equation token : " + token);
				}
				
				if (hasRP) {
					itemSqls.add(")");
				}
			}
			return "( " + StringUtils.join(itemSqls, " ") + " )";
		}
	}
	
	/**
	 * @param item
	 * @param values
	 * @return
	 */
	private String parseItem(JSONObject item, JSONObject values) {
		String field = item.getString("field");
		boolean hasAndFlag = field.startsWith("&");
		if (hasAndFlag) {
			field = field.substring(1);
		}
		
		final String[] fieldPath = field.split("\\.");
		if (fieldPath.length > 2) {
			throw new IllegalParameterException("Unsupportted joins : " + field);
		}
		
		if (!rootEntity.containsField(fieldPath[0])) {
			LOG.warn("Unknow field '" + field + "' in '" + rootEntity.getName() + "'");
			return null;
		}
		
		Field fieldMeta = rootEntity.getField(fieldPath[0]);
		if (fieldPath.length > 1) {
			if (EasyMeta.getDisplayType(fieldMeta) != DisplayType.REFERENCE) {
				throw new IllegalParameterException("Non reference-field : " + field);
			}
			fieldMeta = fieldMeta.getReferenceEntity().getField(fieldPath[1]);
		}
		
		DisplayType dt = EasyMeta.getDisplayType(fieldMeta);
		// TODO 分类字段仅能查询最后一级
		if (dt == DisplayType.CLASSIFICATION || hasAndFlag) {
			field = "&" + field;
		}
		
		final String op = item.getString("op");
		
		StringBuffer sb = new StringBuffer(field)
				.append(' ')
				.append(convOp(op))
				.append(' ');
		if (op.equalsIgnoreCase("NL") || op.equalsIgnoreCase("NT")) {
			return sb.toString();
		}
		
		String value = item.getString("value");
		
		// TODO 自定义函数
		String zeroTime = " 00:00:00";
		String fullTime = " 23:59:59";
		if ("BFD".equalsIgnoreCase(op)) {
			value = CalendarUtils.getUTCDateFormat().format(CalendarUtils.addDay(-NumberUtils.toInt(value))) + fullTime;
		} else if ("AFD".equalsIgnoreCase(op)) {
			value = CalendarUtils.getUTCDateFormat().format(CalendarUtils.addDay(NumberUtils.toInt(value))) + zeroTime;
		} else if ("BFM".equalsIgnoreCase(op)) {
			value = CalendarUtils.getUTCDateFormat().format(CalendarUtils.addMonth(-NumberUtils.toInt(value))) + fullTime;
		} else if ("AFM".equalsIgnoreCase(op)) {
			value = CalendarUtils.getUTCDateFormat().format(CalendarUtils.addMonth(NumberUtils.toInt(value))) + zeroTime;
		} else if ("RED".equalsIgnoreCase(op)) {
			value = CalendarUtils.getUTCDateFormat().format(CalendarUtils.addDay(-NumberUtils.toInt(value))) + fullTime;
		} else if ("REM".equalsIgnoreCase(op)) {
			value = CalendarUtils.getUTCDateFormat().format(CalendarUtils.addMonth(-NumberUtils.toInt(value))) + fullTime;
		} else if ("SFU".equalsIgnoreCase(op)) {
			value = Application.getCurrentUser().toLiteral();
		} else if ("SFB".equalsIgnoreCase(op)) {
			Department dept = UserHelper.getDepartment(Application.getCurrentUser());
			if (dept != null) {
				value = dept.getIdentity().toString();
				int refe = fieldMeta.getReferenceEntity().getEntityCode();
				if (refe == EntityHelper.User) {
					sb.insert(sb.indexOf(" "), ".deptId");
				} else if (refe == EntityHelper.Department) {
					// Nothings
				} else {
					value = null;
				}
			}
		} else if ("SFD".equalsIgnoreCase(op)) {
			Department dept = UserHelper.getDepartment(Application.getCurrentUser());
			if (dept != null) {
				int refe = fieldMeta.getReferenceEntity().getEntityCode();
				if (refe == EntityHelper.Department) {
					value = StringUtils.join(UserHelper.getAllChildren(dept), "|");
				}
			}
		}
				
		if (StringUtils.isBlank(value)) {
			LOG.warn("Invalid item of AdvFilter : " + item.toJSONString());
			return null;
		}
		
		// 占位 {1}
		if (value.matches("\\{\\d+\\}")) {
			if (values == null) {
				LOG.warn("Invalid item of AdvFilter : " + item.toJSONString());
				return null;
			}
			
			String valHold = value.replaceAll("[\\{\\}]", "");
			value = parseValue(values.get(valHold), op, fieldMeta);
		} else {
			value = parseValue(value, op, fieldMeta);
		}
		
		// No value
		if (value == null) {
			return null;
		}
		
		// 区间
		boolean isBetween = op.equalsIgnoreCase("BW");
		String value2 = isBetween ? parseValue(item.getString("value2"), op, fieldMeta) : null;
		if (isBetween && value2 == null) {
			value2 = value;
		}
		
		if (op.equalsIgnoreCase("IN") || op.equalsIgnoreCase("NIN") || op.equalsIgnoreCase("SFD")) {
			sb.append(value);
		} else {
			if (op.equalsIgnoreCase("LK") || op.equalsIgnoreCase("NLK")) {
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
	 * @param field
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
			if (op.equalsIgnoreCase("IN") || op.equalsIgnoreCase("NIN") || op.equalsIgnoreCase("SFD")) {
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
		else if ("BFD".equalsIgnoreCase(op)) return "<=";  // "$before_day(%d)";
		else if ("BFM".equalsIgnoreCase(op)) return "<=";  // "$before_month(%d)";
		else if ("AFD".equalsIgnoreCase(op)) return ">=";  // "$after_day(%d)";
		else if ("AFM".equalsIgnoreCase(op)) return ">=";  // "$after_month(%d)";
		else if ("RED".equalsIgnoreCase(op)) return ">";   // "$recent_day(%d)";
		else if ("REM".equalsIgnoreCase(op)) return ">";   // "$recent_month(%d)";
		else if ("SFU".equalsIgnoreCase(op)) return "=";
		else if ("SFB".equalsIgnoreCase(op)) return "=";
		else if ("SFD".equalsIgnoreCase(op)) return "in";
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
	
	/**
	 * @param qFields
	 * @return
	 */
	private JSONArray buildQuickFilterItems(String qFields) {
		final Set<String> fieldItems = new HashSet<>();
		
		// 指定字段
		if (StringUtils.isNotBlank(qFields)) {
			for (String field : qFields.split(",")) {
				field = field.trim();
				if (MetadataHelper.getLastJoinField(rootEntity, field) != null) {
					fieldItems.add(field);
				} else {
					LOG.warn("No field found by QuickFilter : " + field + " in " + rootEntity.getName());
				}
			}
		}
		
		// 追加名称字段和 quickCode
		Field nameField = rootEntity.getNameField();
		DisplayType dt = EasyMeta.getDisplayType(nameField);
		
		// 引用字段不能作为名称字段，此处的处理是因为某些系统实体有用到
		// 请主要要保证其兼容 LIKE 条件的语法要求
		if (dt == DisplayType.REFERENCE) {
			fieldItems.add("&" + nameField.getName());
		} else if (dt == DisplayType.PICKLIST || dt == DisplayType.CLASSIFICATION) {
			fieldItems.add("&" + nameField.getName());
		} else if (dt == DisplayType.TEXT || dt == DisplayType.EMAIL || dt == DisplayType.URL || dt == DisplayType.PHONE || dt == DisplayType.SERIES) {
			fieldItems.add(nameField.getName());
		}
		
		if (rootEntity.containsField(EntityHelper.QuickCode)) {
			fieldItems.add(EntityHelper.QuickCode);
		}
		
		JSONArray items = new JSONArray();
		for (String field : fieldItems) {
			items.add(JSON.parseObject("{ op:'lk', value:'{1}', field:'" + field + "' }"));
		}
		return items;
	}
	
	/**
	 * 测试高级表达式
	 * 
	 * @param equation
	 * @return null 表示无效
	 */
	public static String validEquation(final String equation) {
		if (StringUtils.isBlank(equation)) {
			return "OR";
		}
		if ("OR".contentEquals(equation) || "AND".equalsIgnoreCase(equation)) {
			return equation;
		}
		
		String clearEquation = equation.toUpperCase().replace("  ", "").trim();
		if (clearEquation.startsWith("AND") || clearEquation.startsWith("OR") || clearEquation.endsWith("AND") || clearEquation.endsWith("OR")) {
			return null;
		}
		if (clearEquation.contains("()") || clearEquation.contains("( )")) {
			return null;
		}
		
		for (String token : clearEquation.split(" ")) {
			token = token.replace("(", "");
			token = token.replace(")", "");
			
			// 数字不能大于 10
			if (NumberUtils.isNumber(token)) {
				if (NumberUtils.toInt(token) > 10) {
					return null;
				} else {
					// 允许
				}
			} else if ("AND".equals(token) || "OR".equals(token) || "(".equals(token) || ")".equals(token)) {
				// 允许
			} else {
				return null;
			}
		}
		
		// 去除 AND OR 0-9 及空格
		clearEquation = clearEquation.replaceAll("[AND|OR|0-9| ]", "");
		// 括弧成对出现
		for (int i = 0; i < 20; i++) {
			clearEquation = clearEquation.replace("()", "");
			if (clearEquation.length() == 0) {
				return equation;
			}
		}
		return null;
	}
}
