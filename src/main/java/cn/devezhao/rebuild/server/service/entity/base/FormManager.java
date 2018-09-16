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

package cn.devezhao.rebuild.server.service.entity.base;

import java.util.Date;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.metadata.ExtRecordCreator;
import cn.devezhao.rebuild.server.service.entitymanage.DisplayType;
import cn.devezhao.rebuild.server.service.entitymanage.EasyMeta;

/**
 * 表单布局管理
 * 
 * @author zhaofang123@gmail.com
 * @since 08/30/2018
 */
public class FormManager extends LayoutConfigManager {

	/**
	 * @param entity
	 * @return
	 */
	public static JSON getFormLayoutRaw(String entity) {
		Object[] lcr = getLayoutConfigRaw(entity, TYPE_FORM);
		JSONObject config = new JSONObject();
		config.put("entity", entity);
		if (lcr != null) {
			config.put("id", lcr[0].toString());
			config.put("elements", lcr[1]);
			return config;
		}
		config.put("elements", new String[0]);
		return config;
	}
	
	/**
	 * @param entity
	 * @return
	 */
	public static JSON getFormLayout(String entity, ID user) {
		return getFormLayout(entity, user, null);
	}
	
	/**
	 * @param entity
	 * @param user
	 * @param recordId
	 * @return
	 */
	public static JSON getFormLayout(String entity, ID user, ID recordId) {
		final Date now = CalendarUtils.now();
		final Entity entityMeta = EntityHelper.getEntity(entity);
		
		JSONObject config = (JSONObject) getFormLayoutRaw(entity);
		for (Object element : config.getJSONArray("elements")) {
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
				if (!el.containsKey("dateFormat")) {
					el.put("dateFormat", "yyyy-MM-dd HH:mm:ss");
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
				if (fieldName.equals(ExtRecordCreator.createdOn) || fieldName.equals(ExtRecordCreator.modifiedOn)) {
					el.put("value", CalendarUtils.getUTCDateTimeFormat().format(now));
				} else if (fieldName.equals(ExtRecordCreator.createdBy) || fieldName.equals(ExtRecordCreator.modifiedBy) || fieldName.equals(ExtRecordCreator.owningUser)) {
					el.put("value", user);
				} else if (fieldName.equals(ExtRecordCreator.owningDept)) {
					el.put("value", "所属部门");
				}
			}
		}
		return config;
	}
	
	/**
	 * 视图布局
	 * 
	 * @param entity
	 * @param recordId
	 * @return
	 */
	public static JSON getViewLayout(String entity, ID recordId) {
		return null;
	}
}
