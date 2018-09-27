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

package com.rebuild.server.service.base;

import java.util.Date;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.privileges.User;
import com.rebuild.server.service.entitymanage.DisplayType;
import com.rebuild.server.service.entitymanage.EasyMeta;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;

/**
 * 表单布局管理
 * 
 * @author zhaofang123@gmail.com
 * @since 08/30/2018
 */
public class FormManager extends LayoutManager {

	/**
	 * @param entity
	 * @return
	 */
	public static JSON getFormLayout(String entity) {
		Object[] raw = getLayoutConfigRaw(entity, TYPE_FORM);
		JSONObject config = new JSONObject();
		config.put("entity", entity);
		if (raw != null) {
			config.put("id", raw[0].toString());
			config.put("elements", raw[1]);
			return config;
		}
		config.put("elements", new String[0]);
		return config;
	}
	
	/**
	 * @param entity
	 * @return
	 */
	public static JSON getFormModal(String entity, ID user) {
		return getFormModal(entity, user, null);
	}
	
	/**
	 * @param entity
	 * @param user
	 * @param recordId
	 * @return
	 */
	public static JSON getFormModal(String entity, ID user, ID recordId) {
		final Entity entityMeta = MetadataHelper.getEntity(entity);
		final User currentUser = Application.getUserStore().getUser(user);
		final Date now = CalendarUtils.now();
		
		JSONObject config = (JSONObject) getFormLayout(entity);
		JSONArray elements = config.getJSONArray("elements");
		for (Object element : elements) {
			JSONObject el = (JSONObject) element;
			String fieldName = el.getString("field");
			Field fieldMeta = entityMeta.getField(fieldName);
			EasyMeta easyField = new EasyMeta(fieldMeta);
			el.put("label", easyField.getLabel());
			String dt = easyField.getDisplayType(false);
			el.put("type", dt);
			el.put("nullable", fieldMeta.isNullable());
			el.put("updatable", fieldMeta.isUpdatable());
			el.put("creatable", true);
			
			// 针对字段的配置
			
			JSONObject ext = easyField.getFieldExtConfig();
			for (Map.Entry<String, Object> e : ext.entrySet()) {
				el.put(e.getKey(), e.getValue());
			}
			
			// 不同类型的特殊处理
			
			if (DisplayType.PICKLIST.name().equals(dt)) {
				JSONArray picklist = PickListManager.getPickList(fieldMeta);
				el.put("options", picklist);
			}
			else if (DisplayType.DATETIME.name().equals(dt)) {
				if (!el.containsKey("datetimeFormat")) {
					el.put("datetimeFormat", "yyyy-MM-dd HH:mm:ss");
				}
			}
			else if (DisplayType.DATE.name().equals(dt)) {
				if (!el.containsKey("dateFormat")) {
					el.put("dateFormat", "yyyy-MM-dd");
				}
			}
			
			// 默认值
			
			if (easyField.isBuiltin()) {
				el.put("creatable", false);
				
				if (fieldName.equals(EntityHelper.createdOn) || fieldName.equals(EntityHelper.modifiedOn)) {
					el.put("value", CalendarUtils.getUTCDateTimeFormat().format(now));
				} else if (fieldName.equals(EntityHelper.createdBy) || fieldName.equals(EntityHelper.modifiedBy) || fieldName.equals(EntityHelper.owningUser)) {
					el.put("value", currentUser.getFullName());
				} else if (fieldName.equals(EntityHelper.owningDept)) {
					el.put("value", currentUser.getOwningDept().getName());
				}
			}
		}
		return config;
	}
	
	// --
	
	/**
	 * @param entity
	 * @return
	 */
	public static JSON getViewLayout(String entity) {
		Object[] raw = getLayoutConfigRaw(entity, TYPE_VIEW);
		JSONObject config = new JSONObject();
		config.put("entity", entity);
		if (raw != null) {
			config.put("id", raw[0].toString());
			config.put("elements", raw[1]);
			return config;
		}
		config.put("elements", new String[0]);
		return config;
	}
	
	/**
	 * 视图布局
	 * 
	 * @param entity
	 * @param user
	 * @param recordId
	 * @return
	 */
	public static JSON getViewModal(String entity, ID user, ID recordId) {
		final Entity entityMeta = MetadataHelper.getEntity(entity);
		
		JSONObject config = (JSONObject) getViewLayout(entity);
		JSONArray elements = config.getJSONArray("elements");
		Record record = null;
		if (!elements.isEmpty()) {
			record = record(recordId, elements);
		}
		
		for (Object element : elements) {
			JSONObject el = (JSONObject) element;
			String fieldName = el.getString("field");
			if (fieldName.equals("$LINE$")) {
				continue;
			}
			
			Field fieldMeta = entityMeta.getField(fieldName);
			EasyMeta easyField = new EasyMeta(fieldMeta);
			el.put("label", easyField.getLabel());
			String dt = easyField.getDisplayType(false);
			el.put("type", dt);
			
			// 填充值
			if (record.hasValue(fieldName)) {
				Object value = record.getObjectValue(fieldName);
				if (easyField.getDisplayType() == DisplayType.PICKLIST) {
					ID pickValue = (ID) value;
					el.put("value", pickValue.getLabel());
				} 
				else if (value instanceof ID) {
					ID idValue = (ID) value;
					String belongEntity = MetadataHelper.getEntity(idValue.getEntityCode()).getName();
					el.put("value", new String[] { idValue.toLiteral(), idValue.getLabel(), belongEntity });
				} 
				else {
					Object human = FieldValueWrapper.wrapFieldValue(value, easyField);
					el.put("value", human);
				}
			}
		}
		return config;
	}
	
	/**
	 * @param id
	 * @param elements
	 * @return
	 */
	protected static Record record(ID id, JSONArray elements) {
		Entity entity = MetadataHelper.getEntity(id.getEntityCode());
		StringBuffer ajql = new StringBuffer("select ");
		for (Object element : elements) {
			JSONObject el = (JSONObject) element;
			String field = el.getString("field");
			if (!entity.containsField(field)) {
				continue;
			}
			
			Field fieldMeta = entity.getField(field);
			if (fieldMeta.getType() == FieldType.REFERENCE) {
				ajql.append('&').append(field).append(',');
			}
			ajql.append(field).append(',');
		}
		ajql.deleteCharAt(ajql.length() - 1);
		ajql.append(" from ").append(entity.getName())
				.append(" where ").append(entity.getPrimaryField().getName())
				.append(" = '").append(id).append("'");
		
		return Application.getQueryFactory().record(ajql.toString());
	}
}
