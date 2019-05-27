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

package com.rebuild.web.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.portals.PickListManager;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 元数据获取
 * 
 * @author zhaofang123@gmail.com
 * @since 09/19/2018
 */
@Controller
@RequestMapping("/commons/metadata/")
public class MetadataGetting extends BaseControll {

	@RequestMapping("entities")
	public void entities(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		List<Map<String, String>> list = new ArrayList<>();
		for (Entity e : MetadataSorter.sortEntities(user)) {
			// 不返回明细实体
			if (e.getMasterEntity() != null) {
				continue;
			}
			
			Map<String, String> map = new HashMap<>();
			EasyMeta easy = new EasyMeta(e);
			map.put("name", e.getName());
			map.put("label", easy.getLabel());
			map.put("icon", easy.getIcon());
			list.add(map);
		}
		writeSuccess(response, list);
	}
	
	@RequestMapping("fields")
	public void fields(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		Entity entityBase = MetadataHelper.getEntity(entity);
		
		boolean deep = "2".equals(getParameter(request, "deep"));
		List<Map<String, Object>> list = new ArrayList<>();
		putFields(list, entityBase, deep, null);

		if (deep) {
			for (Field field : entityBase.getFields()) {
				EasyMeta easyField = EasyMeta.valueOf(field);
				if (easyField.getDisplayType() == DisplayType.REFERENCE
						&& !MetadataHelper.isBizzEntity(field.getReferenceEntity().getEntityCode())) {
					// 显示父级字段
					Map<String, Object> parent = new HashMap<>();
					parent.put("name", field.getName());
					parent.put("label", easyField.getLabel());
					parent.put("type", easyField.getDisplayType().name());
					parent.put("creatable", field.isCreatable());
					list.add(parent);
					
					putFields(list, field.getReferenceEntity(), false, easyField);
				}
			}
		}

		writeSuccess(response, list);
	}

	/**
	 * @param dest
	 * @param entity
	 * @param onlyBizzRefField
	 * @param parentField
	 */
	private void putFields(List<Map<String, Object>> dest, Entity entity, boolean onlyBizzRefField, EasyMeta parentField) {
		for (Field field : MetadataSorter.sortFields(entity)) {
			Map<String, Object> map = new HashMap<>();
			map.put("name", field.getName());
			map.put("creatable", field.isCreatable());
			EasyMeta easyMeta = new EasyMeta(field);
			map.put("label", easyMeta.getLabel());
			DisplayType dt = easyMeta.getDisplayType();
			map.put("type", dt.name());
			if (dt == DisplayType.REFERENCE) {
				Entity refEntity = field.getReferenceEntity();
				boolean isBizz = MetadataHelper.isBizzEntity(refEntity.getEntityCode());
				if (onlyBizzRefField && !isBizz) {
					continue;
				}
				
				Field refNameField  = MetadataHelper.getNameField(refEntity);
				map.put("ref", new String[] { refEntity.getName(), EasyMeta.getDisplayType(refNameField).name() });
				// Fix fieldType to nameField
				if (!isBizz) {
					map.put("type", EasyMeta.getDisplayType(refNameField));
				}
			}

			if (parentField != null) {
				map.put("name", parentField.getName() + "." + map.get("name"));
				map.put("label", parentField.getLabel() + "." + map.get("label"));
			}

			dest.add(map);
		}
	}
	
	// 哪些实体引用了指定实体
	@RequestMapping("references")
	public void references(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		Set<Entity> references = new HashSet<>();
		for (Field field : entityMeta.getReferenceToFields()) {
			Entity own = field.getOwnEntity();
			if (!MetadataHelper.isSlaveEntity(own.getEntityCode())) {
				references.add(own);
			}
		}
		
		List<String[]> list = new ArrayList<>();
		for (Entity e : references) {
			EasyMeta easy = new EasyMeta(e);
			list.add(new String[] { easy.getName(), easy.getLabel() });
		}
		writeSuccess(response, list);
	}
	
	// PickList 值列表
	@RequestMapping("picklist")
	public void ref(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		String field = getParameterNotNull(request, "field");
		JSON list = PickListManager.getPickList(entity, field, false);
		writeSuccess(response, list);
	}
}
