/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package cn.devezhao.rebuild.web.base.entity.datalist;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.query.compiler.SelectItem;
import cn.devezhao.rebuild.server.metadata.MetadataHelper;

/**
 * 数据包装
 * 
 * @author Zhao Fangfang
 * @version $Id: DataWrapper.java 1 2014-11-26 17:20:23Z zhaoff@qidapp.com $
 * @since 1.0, 2013-6-20
 */
public class DataWrapper {

	private int total;
	private Object[][] data;
	private SelectItem[] selectFields;
	
	/**
	 * @param total
	 * @param data
	 * @param fields
	 */
	public DataWrapper(int total, Object[][] data, SelectItem[] fields) {
		this.total = total;
		this.data = data;
		this.selectFields = fields;
	}
	
	/**
	 * @return
	 */
	public String toJson() {
		for (Object[] o : data) {
			for (int i = 0; i < selectFields.length; i++) {
				Field field = selectFields[i].getField();
				Type cType = field.getType();
				if (cType == FieldType.DATE) {
					o[i] = wrapDate(o[i], field);
				} else if (cType == FieldType.TIMESTAMP) {
					o[i] = wrapDatetime(o[i], field);
				} else if (cType == FieldType.INT || cType == FieldType.LONG) {
					o[i] = wrapNumber(o[i], field);
				} else if (cType == FieldType.DOUBLE || cType == FieldType.DECIMAL) {
					o[i] = wrapDecimal(o[i], field);
				} else if (cType == FieldType.BOOL) {
					o[i] = wrapBool(o[i], field);
				} else {
					o[i] = wrapSimple(o[i], field);
				}
			}
		}
		
		Map<String, Object> map = new HashMap<>();
		map.put("total", total);
		map.put("data", data);
		return JSON.toJSONString(map);
	}
	
	/**
	 * @param date
	 * @param field
	 * @return
	 */
	protected Object wrapDate(Object date, Field field) {
		if (date == null) {
			return StringUtils.EMPTY;
		}
		return CalendarUtils.getUTCDateFormat().format(date);
	}

	/**
	 * @param date
	 * @param field
	 * @return
	 */
	protected Object wrapDatetime(Object date, Field field) {
		if (date == null) {
			return StringUtils.EMPTY;
		}
		return CalendarUtils.getUTCDateTimeFormat().format(date);
	}
	
	/**
	 * @param number
	 * @param field
	 * @return
	 */
	protected Object wrapNumber(Object number, Field field) {
		if (number == null) {
			return StringUtils.EMPTY;
		}
		String fv = new DecimalFormat("##,###").format(number);
		return StringUtils.isEmpty(fv) ? "0" : fv;
	}

	/**
	 * @param number
	 * @param field
	 * @return
	 */
	protected Object wrapDecimal(Object number, Field field) {
		if (number == null) {
			return StringUtils.EMPTY;
		}
		String fv = new DecimalFormat("##,##0.00").format(number);
		return StringUtils.equals(fv, "0.00") ? "0" : fv;
	}

	/**
	 * @param bool
	 * @param field
	 * @return
	 */
	protected Object wrapBool(Object bool, Field field) {
		return bool == null ? StringUtils.EMPTY : ((Boolean) bool ? "是" : "否");
	}

	/**
	 * @param value
	 * @param field
	 * @return
	 */
	protected Object wrapSimple(Object value, Field field) {
		if (value == null) {
			return StringUtils.EMPTY;
		}
		
		if (value instanceof Object[]) {
			Object[] idNamed = (Object[]) value;
			Object[] idNamed2 = new Object[3];
			Entity idEntity = MetadataHelper.getEntity(((ID) idNamed[0]).getEntityCode());
			idNamed2[2] = idEntity.getName();
			idNamed2[1] = idNamed[1] == null ? StringUtils.EMPTY : idNamed[1].toString();
			idNamed2[0] = idNamed[0].toString();
			return idNamed2;
		} else {
			return value.toString();
		}
	}
}
