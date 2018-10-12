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

package com.rebuild.web.admin.entityhub;

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
import com.rebuild.server.Application;
import com.rebuild.server.entityhub.AccessibleMeta;
import com.rebuild.server.entityhub.Entity2Schema;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

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
		for (Entity entity : MetadataHelper.getEntities()) {
			AccessibleMeta easyMeta = new AccessibleMeta(entity);
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
		
		Application.getMetadataFactory().refresh(false);
		writeSuccess(response);
	}
	
	/**
	 * @param mv
	 * @param entity
	 * @return
	 */
	protected static AccessibleMeta setEntityBase(ModelAndView mv, String entity) {
		Entity e = MetadataHelper.getEntity(entity);
		AccessibleMeta entityMeta = new AccessibleMeta(e);
		mv.getModel().put("entityMetaId", entityMeta.getMetaId());
		mv.getModel().put("entityName", entityMeta.getName());
		mv.getModel().put("entityLabel", entityMeta.getLabel());
		mv.getModel().put("icon", entityMeta.getIcon());
		mv.getModel().put("comments", entityMeta.getComments());
		mv.getModel().put("nameField", e.getNameField().getName());
		return entityMeta;
	}
}
