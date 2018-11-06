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
import com.rebuild.server.entityhub.EasyMeta;
import com.rebuild.server.helper.manager.PickListManager;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.PortalMetaSorter;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 09/19/2018
 */
@Controller
@RequestMapping("/commons/metadata/")
public class MetadataGet extends BaseControll {

	@RequestMapping("entities")
	public void entities(HttpServletRequest request, HttpServletResponse response) throws IOException {
		List<Map<String, String>> list = new ArrayList<>();
		for (Entity e : PortalMetaSorter.sortEntities()) {
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
		
		List<Map<String, String>> list = new ArrayList<>();
		for (Field e : PortalMetaSorter.sortFields(entityBase)) {
			Map<String, String> map = new HashMap<>();
			map.put("name", e.getName());
			EasyMeta easyMeta = new EasyMeta(e);
			map.put("label", easyMeta.getLabel());
			map.put("type", easyMeta.getDisplayType(false));
			list.add(map);
		}
		writeSuccess(response, list);
	}
	
	// 指定实体所引用的字段实体
	@RequestMapping("references")
	public void references(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		Set<Entity> references = new HashSet<>();
		
		Field[] rtFields = entityMeta.getReferenceToFields();
		for (Field field : rtFields) {
			Entity own = field.getOwnEntity();
			references.add(own);
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
