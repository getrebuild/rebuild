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

package com.rebuild.server.configuration.portals;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;

/**
 * 表单默认值
 * 
 * @author zhaofang123@gmail.com
 * @since 11/15/2018
 */
public class FormDefaultValue {
	
	private static final Log LOG = LogFactory.getLog(FormDefaultValue.class);
	
	public static final String DV_MASTER = "$MASTER$";
	public static final String DV_REFERENCE_PREFIX = "&";
	
	/**
	 * @param field
	 * @return
	 */
	public static Object exprDefaultValue(Field field) {
		return exprDefaultValue(field, (String) field.getDefaultValue());
	}
	
	/**
	 * @param field
	 * @param valueExpr
	 * @return
	 */
	public static Object exprDefaultValue(Field field, String valueExpr) {
		if (StringUtils.isBlank(valueExpr)) {
			return null;
		}
		
		if (field.getType() == FieldType.TIMESTAMP || field.getType() == FieldType.DATE) {
			if ("{NOW}".equals(valueExpr)) {
				return CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now());
			}
			
			Pattern exprPattern = Pattern.compile("\\{NOW([-+])([0-9]{1,9})([YMDH])\\}");
			Matcher exprMatcher = exprPattern.matcher(StringUtils.remove(valueExpr, " "));
			if (exprMatcher.matches()) {
				String op = exprMatcher.group(1);
				String num = exprMatcher.group(2);
				String unit = exprMatcher.group(3);
				int num2int = ObjectUtils.toInt(num);
				if ("-".equals(op)) {
					num2int = -num2int;
				}
				
				Date date = null;
				if (num2int == 0) {
					date = CalendarUtils.now();
				} else if ("Y".equals(unit)) {
					date = CalendarUtils.add(num2int, Calendar.YEAR);
				} else if ("M".equals(unit)) {
					date = CalendarUtils.add(num2int, Calendar.MONTH);
				} else if ("D".equals(unit)) {
					date = CalendarUtils.add(num2int, Calendar.DAY_OF_MONTH);
				} else if ("H".equals(unit)) {
					date = CalendarUtils.add(num2int, Calendar.HOUR_OF_DAY);
				}
				return date == null ? null : CalendarUtils.getUTCDateTimeFormat().format(date);
			} else {
				String format = "yyyy-MM-dd HH:mm:ss".substring(0, valueExpr.length());
				if (CalendarUtils.parse(valueExpr, format) != null) {
					return valueExpr;
				} else {
					return null;
				}
			}
		}
		// Others here
		else {
			return valueExpr;
		}
	}

	/**
	 * @param entity
	 * @param formModel
	 * @param initialVal 此值优先级大于字段默认值
	 */
	public static void setFormInitialValue(Entity entity, JSON formModel, JSONObject initialVal) {
		final JSONArray elements = ((JSONObject) formModel).getJSONArray("elements");
		if (elements == null || initialVal == null || initialVal.isEmpty()) {
			return;
		}
		
		final Map<String, Object> valuesReady = new HashMap<>();
		for (Map.Entry<String, Object> e : initialVal.entrySet()) {
			String field = e.getKey();
			String value = (String) e.getValue();
			if (StringUtils.isBlank(value)) {
				continue;
			}
			
			// 引用字段实体，如 `&User`
			if (field.startsWith(DV_REFERENCE_PREFIX)) {
				Object idLabel[] = readyReferenceValue(value);
				if (idLabel != null) {
					Entity source = MetadataHelper.getEntity(field.substring(1));
					Field[] reftoFields = MetadataHelper.getReferenceToFields(source, entity);
					// 如有多个则全部填充
					for (Field rtf : reftoFields) {
						valuesReady.put(rtf.getName(), idLabel);
					}
				}
			}
			// 主实体字段
			else if (field.equals(DV_MASTER)) {
				Object idLabel[] = readyReferenceValue(value);
				if (idLabel != null) {
					Field stm = MetadataHelper.getSlaveToMasterField(entity);
					valuesReady.put(stm.getName(), idLabel);
				}
			}
			else if (entity.containsField(field)) {
				EasyMeta fieldMeta = EasyMeta.valueOf(entity.getField(field));
				if (fieldMeta.getDisplayType() == DisplayType.REFERENCE) {
					Object idLabel[] = readyReferenceValue(value);
					if (idLabel != null) {
						valuesReady.put(field, readyReferenceValue(value));
					}
				}
			} else {
				LOG.warn("Unknow value pair : " + field + " = " + value);
			}
		}
		
		// TODO 后台设置的默认值，应该在后台处理 ???
		
		if (valuesReady.isEmpty()) {
			return;
		}
		
		for (Object o : elements) {
			JSONObject item = (JSONObject) o;
			String field = item.getString("field");
			if (valuesReady.containsKey(field)) {
				item.put("value", valuesReady.get(field));
				valuesReady.remove(field);
			}
		}
		
		// 还有没布局出来的也返回
		if (!valuesReady.isEmpty()) {
			JSONObject initial = new JSONObject();
			for (Map.Entry<String, Object> e : valuesReady.entrySet()) {
				Object v = e.getValue();
				if (v instanceof Object[]) {
					v = ((Object[]) v)[0].toString();
				}
				initial.put(e.getKey(), v);
			}
			((JSONObject) formModel).put("initialValue", initial);
		}
	}
	
	/**
	 * @param idVal
	 * @return
	 */
	private static Object[] readyReferenceValue(String idVal) {
		if (!ID.isId(idVal)) {
			return null;
		}
		try {
			String label = FieldValueWrapper.getLabel(ID.valueOf(idVal));
			return new Object[] { idVal, label };
		} catch (NoRecordFoundException ex) {
			LOG.error("No record found : " + idVal);
			return null;
		}
	}
}
