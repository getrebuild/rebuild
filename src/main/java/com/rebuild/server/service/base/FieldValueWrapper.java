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

package com.rebuild.server.service.base;

import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.entitymanage.DisplayType;
import com.rebuild.server.service.entitymanage.EasyMeta;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * 字段值包装
 * 
 * @author zhaofang123@gmail.com
 * @since 09/23/2018
 */
public class FieldValueWrapper {

	/**
	 * @param date
	 * @param field
	 * @return
	 */
	public static Object wrapFieldValue(Object value, EasyMeta field) {
		if (value == null || StringUtils.isBlank(value.toString())) {
			return StringUtils.EMPTY;
		}
		
		DisplayType dt = field.getDisplayType();
		if (dt == DisplayType.DATE) {
			return wrapDate(value, field);
		} else if (dt == DisplayType.DATETIME) {
			return wrapDatetime(value, field);
		} else if (dt == DisplayType.NUMBER) {
			return wrapNumber(value, field);
		} else if (dt == DisplayType.DECIMAL) {
			return wrapDecimal(value, field);
		} else if (dt == DisplayType.REFERENCE) {
			return wrapReference(value, field);
		} else if (/*dt == DisplayType.ID ||*/ dt == DisplayType.PICKLIST 
				|| dt == DisplayType.IMAGE || dt == DisplayType.FILE || dt == DisplayType.LOCATION) {
			// 无需处理
			return value;
		} else {
			return wrapSimple(value, field);
		}
	}
	
	/**
	 * @param date
	 * @param field
	 * @return
	 */
	public static String wrapDate(Object date, EasyMeta field) {
		String format = field.getFieldExtConfig().getString("dateFormat");
		format = StringUtils.defaultIfEmpty(format, field.getDisplayType().getDefaultFormat());
		Assert.notNull(format, "No format : " + field.getBaseMeta());
		return CalendarUtils.getDateFormat(format).format(date);
	}

	/**
	 * @param date
	 * @param field
	 * @return
	 */
	public static String wrapDatetime(Object date, EasyMeta field) {
		String format = field.getFieldExtConfig().getString("datetimeFormat");
		format = StringUtils.defaultIfEmpty(format, field.getDisplayType().getDefaultFormat());
		Assert.notNull(format, "No format : " + field.getBaseMeta());
		return CalendarUtils.getDateFormat(format).format(date);
	}
	
	/**
	 * @param number
	 * @param field
	 * @return
	 */
	public static String wrapNumber(Object number, EasyMeta field) {
		String format = field.getFieldExtConfig().getString("numberFormat");
		format = StringUtils.defaultIfEmpty(format, field.getDisplayType().getDefaultFormat());
		Assert.notNull(format, "No format : " + field.getBaseMeta());
		return new DecimalFormat(format).format(number);
	}

	/**
	 * @param decimal
	 * @param field
	 * @return
	 */
	public static String wrapDecimal(Object decimal, EasyMeta field) {
		String format = field.getFieldExtConfig().getString("decimalFormat");
		format = StringUtils.defaultIfEmpty(format, field.getDisplayType().getDefaultFormat());
		Assert.notNull(format, "No format : " + field.getBaseMeta());
		return new DecimalFormat(format).format(decimal);
	}

	/**
	 * @param reference
	 * @param field
	 * @return
	 */
	public static Object wrapReference(Object reference, EasyMeta field) {
		Object[] referenceValue = (Object[]) reference;
		Object[] idNamed = new Object[3];
		Entity idEntity = MetadataHelper.getEntity(((ID) referenceValue[0]).getEntityCode());
		idNamed[2] = idEntity.getName();
		idNamed[1] = referenceValue[1] == null ? StringUtils.EMPTY : referenceValue[1].toString();
		idNamed[0] = referenceValue[0].toString();
		return idNamed;
	}
	
	/**
	 * @param simple
	 * @param field
	 * @return
	 */
	public static String wrapSimple(Object simple, EasyMeta field) {
		return simple.toString().trim();
	}
}
