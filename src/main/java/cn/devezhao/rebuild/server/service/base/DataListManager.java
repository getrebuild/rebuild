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

package cn.devezhao.rebuild.server.service.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.metadata.MetadataHelper;
import cn.devezhao.rebuild.server.service.entitymanage.EasyMeta;

/**
 * 数据列表
 * 
 * @author zhaofang123@gmail.com
 * @since 08/30/2018
 */
public class DataListManager extends LayoutManager {
	
	private static final Log LOG = LogFactory.getLog(DataListManager.class);

	/**
	 * @param entity
	 * @return
	 */
	public static JSON getColumnLayout(String entity) {
		Object[] raw = getLayoutConfigRaw(entity, TYPE_DATALIST);
		
		List<Map<String, Object>> columnList = new ArrayList<>();
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		Field nameField = entityMeta.getNameField();
		nameField = nameField == null ? entityMeta.getPrimaryField() : nameField;
		
		// 默认配置
		if (raw == null) {
			if (nameField != null) {
				columnList.add(warpColumn(nameField));
			}
			if (entityMeta.containsField(EntityHelper.createdOn)) {
				columnList.add(warpColumn(entityMeta.getField(EntityHelper.createdOn)));
			}
		} else {
			JSONArray config = (JSONArray) raw[1];
			for (Object o : config) {
				JSONObject jo = (JSONObject) o;
				String field = jo.getString("field");
				if (entityMeta.containsField(field)) {
					Map<String, Object> map = warpColumn(entityMeta.getField(field));
					Integer width = jo.getInteger("width");
					if (width != null) {
						map.put("width", width);
					}
					columnList.add(map);
				} else {
					LOG.warn("Invalid field : " + field);
				}
			}
		}
		
		Map<String, Object> ret = new HashMap<>();
		ret.put("entity", entity);
		ret.put("nameField", nameField.getName());
		ret.put("fields", columnList);
		return (JSON) JSON.parse(JSON.toJSONString(ret));
	}
	
	/**
	 * @param field
	 * @return
	 */
	public static Map<String, Object> warpColumn(Field field) {
		EasyMeta easyMeta = new EasyMeta(field);
		Map<String, Object> map = new HashMap<>();
		map.put("field", easyMeta.getName());
		map.put("label", easyMeta.getLabel());
		map.put("type", easyMeta.getDisplayType(false));
		return map;
	}
}
