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

package com.rebuild.server.helper.manager;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.bizz.privileges.User;
import com.rebuild.server.entityhub.DisplayType;
import com.rebuild.server.entityhub.EasyMeta;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;

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

	private static final Log LOG = LogFactory.getLog(FormManager.class);
	
	private static final ThreadLocal<ID> MASTERID4NEWSLAVE = new ThreadLocal<>();
	
	/**
	 * @param entity
	 * @param user
	 * @return
	 */
	public static JSON getFormLayout(String entity, ID user) {
		JSONObject cfgsJson = new JSONObject();
		cfgsJson.put("entity", entity);
		
		Object[] cfgs = getLayoutConfigRaw(entity, TYPE_FORM, user);
		if (cfgs != null) {
			cfgsJson.put("id", cfgs[0].toString());
			cfgsJson.put("elements", cfgs[1]);
		} else {
			cfgsJson.put("elements", ArrayUtils.EMPTY_STRING_ARRAY);
		}
		return cfgsJson;
	}
	
	/**
	 * 表单-新建
	 * 
	 * @param entity
	 * @param user
	 * @return
	 */
	public static JSON getFormModel(String entity, ID user) {
		return getFormModel(entity, user, null);
	}
	
	/**
	 * 表单-编辑
	 * 
	 * @param entity
	 * @param user
	 * @param record
	 * @return
	 */
	public static JSON getFormModel(String entity, ID user, ID record) {
		return getModel(entity, user, record, false);
	}
	
	/**
	 * 视图
	 * 
	 * @param entity
	 * @param user
	 * @param record
	 * @return
	 */
	public static JSON getViewModel(String entity, ID user, ID record) {
		Assert.notNull(record, "[record] not be null");
		return getModel(entity, user, record, true);
	}
	
	/**
	 * @param entity
	 * @param user
	 * @param record
	 * @param onView 视图模式?
	 * @return
	 */
	protected static JSON getModel(String entity, ID user, ID record, boolean onView) {
		Assert.notNull(entity, "[entity] not be null");
		Assert.notNull(user, "[user] not be null");
		
		final Entity entityMeta = MetadataHelper.getEntity(entity);
		final User currentUser = Application.getUserStore().getUser(user);
		final Date now = CalendarUtils.now();
		
		// 判断表单权限
		if (record == null) {
			if (entityMeta.getMasterEntity() != null) {
				ID masterId = MASTERID4NEWSLAVE.get();
				Assert.notNull(masterId, "Call #setCurrentMasterId first");
				MASTERID4NEWSLAVE.set(null);
				
				if (!Application.getSecurityManager().allowedU(user, masterId)) {
					return formatModelError("你没有权限向此记录添加明细");
				}
			} else if (!Application.getSecurityManager().allowedC(user, entityMeta.getEntityCode())) {
				return formatModelError("没有新建权限");
			}
			
		} else {
			if (onView) {
				if (!Application.getSecurityManager().allowedR(user, record)) {
					return formatModelError("你没有读取此记录的权限");
				}
			} else {
				if (!Application.getSecurityManager().allowedU(user, record)) {
					return formatModelError("你没有编辑此记录的权限");
				}
			}
		}
		
		JSONObject config = (JSONObject) getFormLayout(entity, user);
		JSONArray elements = config.getJSONArray("elements");
		
		if (elements == null || elements.isEmpty()) {
			return formatModelError("此表单布局尚未配置，请配置后使用");
		}
		
		Record data = null;
		if (!elements.isEmpty() && record != null) {
			data = queryRecord(record, elements);
			if (data == null) {
				return formatModelError("此记录已被删除，或你对此记录没有读取权限");
			}
		}
		
		for (Object element : elements) {
			JSONObject el = (JSONObject) element;
			String fieldName = el.getString("field");
			
			// 分割线
			if (fieldName.equals("$DIVIDER$")) {
				continue;
			}
			
			Field fieldMeta = entityMeta.getField(fieldName);
			EasyMeta easyField = new EasyMeta(fieldMeta);
			el.put("label", easyField.getLabel());
			DisplayType dt = easyField.getDisplayType();
			el.put("type", dt.name());
			el.put("nullable", fieldMeta.isNullable());
			el.put("readonly", false);
			
			// 不可更新字段
			if (data != null && !fieldMeta.isUpdatable()) {
				el.put("readonly", true);
			}
			
			// 针对字段的配置
			
			JSONObject fieldExt = easyField.getFieldExtConfig();
			for (Map.Entry<String, Object> e : fieldExt.entrySet()) {
				el.put(e.getKey(), e.getValue());
			}
			
			// 不同类型的特殊处理
			
			if (dt == DisplayType.PICKLIST) {
				JSONArray options = PickListManager.getPickList(fieldMeta);
				el.put("options", options);
			}
			else if (dt == DisplayType.DATETIME) {
				if (!el.containsKey("datetimeFormat")) {
					el.put("datetimeFormat", DisplayType.DATETIME.getDefaultFormat());
				}
			}
			else if (dt == DisplayType.DATE) {
				if (!el.containsKey("dateFormat")) {
					el.put("dateFormat", DisplayType.DATE.getDefaultFormat());
				}
			}
			
			// 编辑/视图
			if (data != null) {
				Object value = wrapFieldValue(data, easyField, onView);
				if (value != null) {
					if (dt == DisplayType.BOOL && !onView) {
						value = "是".equals(value) ? "T" : "F";
					}
					
					el.put("value", value);
				}
			}
			// 新建记录
			else {
				if (!fieldMeta.isCreatable()) {
					el.put("readonly", true);
					if (fieldName.equals(EntityHelper.createdOn) || fieldName.equals(EntityHelper.modifiedOn)) {
						el.put("value", CalendarUtils.getUTCDateTimeFormat().format(now));
					} else if (fieldName.equals(EntityHelper.createdBy) || fieldName.equals(EntityHelper.modifiedBy) || fieldName.equals(EntityHelper.owningUser)) {
						el.put("value", new Object[] { currentUser.getIdentity().toString(), currentUser.getFullName(), "User" });
					} else if (fieldName.equals(EntityHelper.owningDept)) {
						el.put("value", new Object[] { currentUser.getOwningDept().getIdentity().toString(), currentUser.getOwningDept().getName(), "Department" });
					}
				}
				
				if (dt == DisplayType.PICKLIST) {
					JSONArray options = el.getJSONArray("options");
					for (Object o : options) {
						JSONObject opt = (JSONObject) o;
						if (opt.getBooleanValue("default")) {
							el.put("value", opt.getString("id"));
							break;
						}
					}
				}
				
				// TODO 字段的默认值
				
			}
		}
		return config;
	}
	
	/**
	 * @param error
	 * @return
	 */
	protected static JSONObject formatModelError(String error) {
		JSONObject cfg = new JSONObject();
		cfg.put("error", error);
		return cfg;
	}
	
	/**
	 * @param id
	 * @param elements
	 * @return
	 */
	protected static Record queryRecord(ID id, JSONArray elements) {
		if (elements.isEmpty()) {
			return null;
		}
		
		Entity entity = MetadataHelper.getEntity(id.getEntityCode());
		StringBuffer ajql = new StringBuffer("select ");
		for (Object element : elements) {
			JSONObject el = (JSONObject) element;
			String field = el.getString("field");
			if (field.startsWith("$")) {
				continue;
			}
			if (!entity.containsField(field)) {
				LOG.warn("Unknow field '" + field + "' in '" + entity.getName() + "'");
				continue;
			}
			
			Field fieldMeta = entity.getField(field);
			
			// PICKLIST and REFERENCE
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
	
	/**
	 * 封装表单/布局所用的字段值
	 * 
	 * @param data
	 * @param field
	 * @param onView
	 * @return
	 */
	protected static Object wrapFieldValue(Record data, EasyMeta field, boolean onView) {
		String fieldName = field.getName();
		if (data.hasValue(fieldName)) {
			Object value = data.getObjectValue(fieldName);
			DisplayType dt = field.getDisplayType();
			if (dt == DisplayType.PICKLIST) {
				ID pickValue = (ID) value;
				return onView ? pickValue.getLabel() : pickValue.toLiteral();
			} 
			else if (value instanceof ID) {
				ID idValue = (ID) value;
				if (onView) {
					String belongEntity = MetadataHelper.getEntity(idValue.getEntityCode()).getName();
					return new String[] { idValue.toLiteral(), idValue.getLabel(), belongEntity };
				} else {
					return new String[] { idValue.toLiteral(), idValue.getLabel() };
				}
			} 
			else {
				Object ret = FieldValueWrapper.wrapFieldValue(value, field);
				// 编辑记录时要去除千分位
				if (!onView && (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL)) {
					ret = ret.toString().replace(",", "");
				}
				return ret;
			}
		}
		return null;
	}
	
	/**
	 * @param masterId
	 */
	public static void setCurrentMasterId(ID masterId) {
		MASTERID4NEWSLAVE.set(masterId);
	}
}
