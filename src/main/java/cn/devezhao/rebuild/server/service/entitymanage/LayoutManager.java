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

package cn.devezhao.rebuild.server.service.entitymanage;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EntityHelper;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
public class LayoutManager {

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
		
		// TODO 暂无布局，使用默认
		
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
			EasyMeta fieldMeta = new EasyMeta(entityMeta.getField(fieldName));
			el.put("label", fieldMeta.getLabel());
			el.put("type", fieldMeta.getDisplayType());
		}
		
		return config;
	}
}
