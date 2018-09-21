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

package cn.devezhao.rebuild.web.admin.entitymanage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.metadata.MetadataHelper;
import cn.devezhao.rebuild.server.service.entitymanage.EasyMeta;
import cn.devezhao.rebuild.server.service.entitymanage.Entity2Schema;
import cn.devezhao.rebuild.web.BaseControll;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
@Controller
@RequestMapping("/admin/")
public class MetaEntityControll extends BaseControll {
	
	@RequestMapping("entities")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		return createModelAndView("/admin/entity/entity-grid.jsp");
	}
	
	@RequestMapping("entity/{entity}/base")
	public ModelAndView pageEntityBase(@PathVariable String entity, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/entity/entity-edit.jsp");
		setEntityBase(mv, entity);
		return mv;
	}

	@RequestMapping("entity/entity-list")
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
	
	@RequestMapping("entity/entity-new")
	public void entityNew(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		String label = getParameterNotNull(request, "label");
		String comments = getParameter(request, "comments");
		
		String entityName = null;
		try {
			entityName = new Entity2Schema(user).create(label, comments);
			writeSuccess(response, entityName);
		} catch (Exception ex) {
			writeFailure(response, ex.getLocalizedMessage());
			return;
		}
	}
	
	@RequestMapping("entity/entity-update")
	public void entityUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);
		Record record = EntityHelper.parse((JSONObject) formJson, user);
		Application.getCommonService().update(record);
		
		MetadataHelper.refreshMetadata();
		writeSuccess(response);
	}
	
	/**
	 * @param mv
	 * @param entity
	 * @return
	 */
	protected static EasyMeta setEntityBase(ModelAndView mv, String entity) {
		Entity e = MetadataHelper.getEntity(entity);
		EasyMeta entityMeta = new EasyMeta(e);
		mv.getModel().put("entityMetaId", entityMeta.getMetaId());
		mv.getModel().put("entityName", entityMeta.getName());
		mv.getModel().put("entityLabel", entityMeta.getLabel());
		mv.getModel().put("icon", entityMeta.getIcon());
		mv.getModel().put("comments", entityMeta.getComments());
		mv.getModel().put("nameField", e.getNameField().getName());
		return entityMeta;
	}
}
