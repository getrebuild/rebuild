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

package cn.devezhao.rebuild.server.service.entity;

import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.service.entitymanage.EasyMeta;

/**
 * 表单管理
 * 
 * @author zhaofang123@gmail.com
 * @since 08/30/2018
 */
public class FormManager {

	/**
	 * 获取表单布局
	 * 
	 * @param entity
	 * @return
	 */
	public static JSON getFormLayout(String entity) {
		Object[] layout = Application.createQuery(
				"select layoutId,config from Layout where type = 1 and entity = ?")
				.setParameter(1, entity)
				.unique();
		JSONObject config = new JSONObject();
		config.put("entity", entity);
		if (layout != null) {
			config.put("id", layout[0].toString());
			config.put("elements", JSON.parseArray((String) layout[1]));
			return config;
		}
		
		config.put("elements", new String[0]);
		return config;
	}
	
	/**
	 * @param entity
	 * @return
	 */
	public static JSON getFormLayoutForPortal(String entity) {
		JSONObject config = (JSONObject) getFormLayout(entity);
		
		Entity entityMeta = EntityHelper.getEntity(entity);
		for (Object element : config.getJSONArray("elements")) {
			JSONObject el = (JSONObject) element;
			String fieldName = el.getString("field");
			Field fieldMeta = entityMeta.getField(fieldName);
			EasyMeta easyField = new EasyMeta(fieldMeta);
			el.put("label", easyField.getLabel());
			el.put("type", easyField.getDisplayType(false));
			el.put("empty", fieldMeta.isNullable());
			el.put("create", easyField.isBuiltin());
			el.put("update", fieldMeta.isUpdatable());
			
			JSONObject ext = easyField.getFieldExtConfig();
			for (Map.Entry<String, Object> e : ext.entrySet()) {
				el.put(e.getKey(), e.getValue());
			}
		}
		return config;
	}
}
