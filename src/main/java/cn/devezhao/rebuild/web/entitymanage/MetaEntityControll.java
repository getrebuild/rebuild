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

package cn.devezhao.rebuild.web.entitymanage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EasyMeta;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.service.entitymanage.Entity2Schema;
import cn.devezhao.rebuild.web.commons.BaseControll;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
@Controller
@RequestMapping("/admin/entity/")
public class MetaEntityControll extends BaseControll {

	@RequestMapping("entity-new")
	public void entityNew(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		String label = getParameterNotNull(request, "label");
		String comments = getParameter(request, "comments");
		
		String entityName = new Entity2Schema(user).create(label, comments);
		if (entityName != null) {
			writeSuccess(response, entityName);
		} else {
			writeFailure(response);
		}
	}
	
	@RequestMapping("list-entity")
	public void listEntity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		List<Map<String, Object>> ret = new ArrayList<>();
		for (Entity entity : Application.getMetadataFactory().getEntities()) {
			EasyMeta easyMeta = new EasyMeta(entity);
			if (easyMeta.isBuiltin()) {
				continue;
			}
			Map<String, Object> map = new HashMap<>();
			map.put("entityName", easyMeta.getName());
			map.put("entityLabel", easyMeta.getLabel());
			map.put("comments", easyMeta.getComments());
			map.put("icon", easyMeta.getIcon());
			ret.add(map);
		}
		writeSuccess(response, ret);
	}
	
	@RequestMapping("baseinfo")
	public void baseInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entityName = getParameter(request, "entity");
		Entity entity = EntityHelper.getEntity(entityName);
		if (entity == null) {
			writeFailure(response, "无效实体");
			return;
		}
		
		EasyMeta easyMeta = new EasyMeta(entity);
		Map<String, Object> ret = new HashMap<>();
		ret.put("entityName", easyMeta.getName());
		ret.put("entityLabel", easyMeta.getLabel());
		ret.put("comments", easyMeta.getComments());
		writeSuccess(response, ret);
	}
	
	@RequestMapping("list-field")
	public void listField(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entityName = getParameter(request, "entity");
		Entity entity = EntityHelper.getEntity(entityName);
		if (entity == null) {
			writeFailure(response, "无效实体");
			return;
		}
		
		List<Map<String, Object>> ret = new ArrayList<>();
		for (Field field : entity.getFields()) {
			EasyMeta easyMeta = new EasyMeta(field);
			if (easyMeta.isBuiltin()) {
				continue;
			}
			
			Map<String, Object> map = new HashMap<>();
			map.put("fieldName", easyMeta.getName());
			map.put("fieldLabel", easyMeta.getLabel());
			map.put("displayType", easyMeta.getDisplayType());
			map.put("comments", easyMeta.getComments());
			ret.add(map);
		}
		writeSuccess(response, ret);
	}
	
	@RequestMapping("list-layout")
	public void listLayout(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO 布局
	}
}
