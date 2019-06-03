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

import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import com.rebuild.server.Application;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.helper.datalist.DataWrapper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 字段值包装。例如 BOOL 类型的 T/F 将格式化为 是/否。
 * 表单/视图/列表等均调用此类，仅在处理特定情景下的特定字段时才需要特殊处理
 * 
 * @author zhaofang123@gmail.com
 * @since 09/23/2018
 * 
 * @see FormsManager
 * @see DataWrapper
 */
public class FieldValueWrapper {
	
	/**
	 * 引用值被删除时的默认显示
	 */
	public static final String MISS_REF_PLACE = "[DELETED]";
	
	/**
	 * 名称字段为空时，采用 @+ID 的方式显示
	 */
	public static final String NO_LABEL_PREFIX = "@";
	
	/**
	 * @param value
	 * @param field
	 * @return
	 */
	public static Object wrapFieldValue(Object value, Field field) {
		return wrapFieldValue(value, new EasyMeta(field));
	}
	
	/**
	 * @param value
	 * @param field
	 * @return
	 */
	public static Object wrapFieldValue(Object value, EasyMeta field) {
		if (value == null || StringUtils.isBlank(value.toString())) {
			return StringUtils.EMPTY;
		}
		
		// 密码型字段返回
		String fieldName = field.getName().toLowerCase();
		if (fieldName.contains("password")) {
			return "******";
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
		} else if (dt == DisplayType.IMAGE || dt == DisplayType.AVATAR
				|| dt == DisplayType.FILE || dt == DisplayType.LOCATION) {
			// 无需处理
			return value;
		} else if (dt == DisplayType.BOOL) {
			return wrapBool(value, field);
		} else if (dt == DisplayType.PICKLIST) {
			return wrapPickList(value, field);
		} else if (dt == DisplayType.CLASSIFICATION) {
			return wrapClassification(value, field);
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
	 * @param reference 接受参数：1.ID; 2.[ID,Label]数组
	 * @param field
	 * @return a String of ID or an array [ID, Label, Entity]
	 */
	public static Object wrapReference(Object reference, EasyMeta field) {
		if (!(reference instanceof Object[])) {
			return reference.toString();
		}
		
		Object[] idLabel = (Object[]) reference;
		Assert.isTrue(idLabel.length == 2, "Must be '[ID, Label]' array");
		
		Object[] idNamed = new Object[3];
		Entity idEntity = MetadataHelper.getEntity(((ID) idLabel[0]).getEntityCode());
		idNamed[2] = idEntity.getName();
		idNamed[1] = idLabel[1] == null ? StringUtils.EMPTY : idLabel[1].toString();
		idNamed[0] = idLabel[0].toString();
		return idNamed;
	}
	
	/**
	 * @param bool
	 * @param field
	 * @return
	 */
	public static String wrapBool(Object bool, EasyMeta field) {
		return ((Boolean) bool) ? "是" : "否";
	}
	
	/**
	 * @param item
	 * @param field
	 * @return
	 * @see PickListManager
	 */
	public static String wrapPickList(Object item, EasyMeta field) {
		return StringUtils.defaultIfBlank(PickListManager.instance.getLabel((ID) item), MISS_REF_PLACE);
	}
	
	/**
	 * @param item
	 * @param field
	 * @return
	 * @see ClassificationManager
	 */
	public static String wrapClassification(Object item, EasyMeta field) {
		return StringUtils.defaultIfBlank(ClassificationManager.instance.getFullName((ID) item), MISS_REF_PLACE);
	}
	
	/**
	 * @param simple
	 * @param field
	 * @return
	 */
	public static String wrapSimple(Object simple, EasyMeta field) {
		String text = simple.toString().trim();
		if (StringUtils.isBlank(text)) {
			return StringUtils.EMPTY;
		} else {
			return text;
		}
	}
	
	// --
	
	/**
	 * 获取记录的 NAME/LABEL 字段值
	 * 
	 * @param id
	 * @return
	 * @throws NoRecordFoundException If no record found
	 */
	public static String getLabel(ID id) throws NoRecordFoundException {
		Entity entity = MetadataHelper.getEntity(id.getEntityCode());
		Field nameField = MetadataHelper.getNameField(entity);
		String sql = "select %s from %s where %s = '%s'";
		sql = String.format(sql, nameField.getName(), entity.getName(), entity.getPrimaryField().getName(), id.toLiteral());
		Object[] label = Application.getQueryFactory().createQueryNoFilter(sql).unique();
		if (label == null) {
			throw new NoRecordFoundException("No label found by ID : " + id);
		}
		
		Object labelVal = FieldValueWrapper.wrapFieldValue(label[0], nameField);
		return labelVal == null ? null : labelVal.toString();
	}
}
