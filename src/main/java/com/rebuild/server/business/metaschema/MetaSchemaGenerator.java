/*
rebuild - Building your system freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.metaschema;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.portals.PickListManager;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;

/**
 * TODO 元数据模型生成
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/28
 */
public class MetaSchemaGenerator {
	
	final private Entity entity;
	
	/**
	 * @param entity
	 */
	public MetaSchemaGenerator(Entity entity) {
		this.entity = entity;
	}
	
	/**
	 * @param dest
	 * @throws IOException 
	 */
	public void generate(File dest) throws IOException {
		JSON schema = generate();
		FileUtils.writeStringToFile(dest, JSON.toJSONString(schema, true), "utf-8");
	}
	
	/**
	 * @return
	 */
	public JSON generate() {
		JSONObject schema = (JSONObject) performEntity(entity, false);
		if (entity.getSlaveEntity() != null) {
			JSON slave = performEntity(entity.getSlaveEntity(), true);
			schema.put("slave", slave);
		}
		return schema;
	}
	
	/**
	 * @param entity
	 * @param slave
	 * @return
	 */
	private JSON performEntity(Entity entity, boolean isSlave) {
		JSONObject schemaEntity = new JSONObject(true);
		
		// 实体
		schemaEntity.put("entity", entity.getName());
		schemaEntity.put("entityLabel", EasyMeta.getLabel(entity));
		JSONArray metaFields = new JSONArray();
		for (Field field : entity.getFields()) {
			if (MetadataHelper.isCommonsField(field)) {
				continue;
			}
			metaFields.add(performField(field));
		}
		schemaEntity.put("fields", metaFields);
		
		// 表单
		// 列表
		// 过滤器
		// 视图相关
		
		return schemaEntity;
	}
	
	/**
	 * @param field
	 * @return
	 */
	private JSON performField(Field field) {
		final JSONObject schemaField = new JSONObject(true);
		final EasyMeta easyField = EasyMeta.valueOf(field);
		final DisplayType dt = easyField.getDisplayType();
		
		schemaField.put("field", easyField.getName());
		schemaField.put("fieldLabel", easyField.getLabel());
		schemaField.put("displayType", dt.name());
		if (easyField.getComments() != null) {
			schemaField.put("comments", easyField.getComments());
		}
		schemaField.put("nullable", field.isNullable());
		schemaField.put("creatable", field.isCreatable());
		schemaField.put("updatable", field.isUpdatable());
		
		if (dt == DisplayType.REFERENCE) {
			schemaField.put("refEntity", field.getReferenceEntity().getName());
			schemaField.put("refEntityLabel", EasyMeta.getLabel(field.getReferenceEntity()));
		} else if (dt == DisplayType.PICKLIST) {
			schemaField.put("items", performPickList(field));
		}
		
		JSONObject extConfig = easyField.getFieldExtConfig();
		if (!extConfig.isEmpty()) {
			schemaField.put("extConfig", extConfig);
		}
		
		return schemaField;
	}
	
	/**
	 * @param field
	 * @return
	 */
	private JSON performPickList(Field field) {
		List<Map<String, Object>> picklist = PickListManager.getPickList(field.getOwnEntity().getName(), field.getName(), false, false);
		JSONArray items = new JSONArray();
		for (Map<String, Object> item : picklist) {
			items.add(new Object[] { item.get("text"), item.get("default") });
		}
		return items;
	}
}
