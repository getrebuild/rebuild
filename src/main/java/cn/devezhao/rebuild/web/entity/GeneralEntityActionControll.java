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

package cn.devezhao.rebuild.web.entity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.util.StringHelper;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.service.entitymanage.LayoutManager;
import cn.devezhao.rebuild.web.commons.BaseControll;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
@Controller
@RequestMapping("/entity/")
public class GeneralEntityActionControll extends BaseControll {

	@RequestMapping("{entity}/new")
	public ModelAndView pageNew(@PathVariable String entity, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/general-entity/entity-new.jsp", entity);
		
		JSON formConfig = LayoutManager.getFormLayoutForPortal(entity);
		mv.getModel().put("FormConfig", formConfig);
		return mv;
	}
	
	@RequestMapping("{entity}/{id}/edit")
	public ModelAndView pageEdit(@PathVariable String entity, @PathVariable String id, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/general-entity/entity-edit.jsp", entity);
		
		JSON formConfig = LayoutManager.getFormLayoutForPortal(entity);
		mv.getModel().put("FormConfig", formConfig);
		return mv;
	}
	
	@RequestMapping("{entity}/{id}")
	public ModelAndView pageView(@PathVariable String entity, @PathVariable String id, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/general-entity/entity-view.jsp", entity);
		
		JSON formConfig = LayoutManager.getFormLayoutForPortal(entity);
		mv.getModel().put("FormConfig", formConfig);
		return mv;
	}
	
	@RequestMapping("form-config")
	public void entityFormConfig(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		JSON fc = LayoutManager.getFormLayoutForPortal(entity);
		writeSuccess(response, fc);
	}
	
	@RequestMapping("record-save")
	public void recordSave(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);
		Record record = EntityHelper.parse((JSONObject) formJson, user);
		record = Application.getCommonService().createOrUpdate(record);
		
		Map<String, Object> map = new HashMap<>();
		map.put("id", record.getPrimary());
		writeSuccess(response, map);
	}
	
	@RequestMapping("record-delete")
	public void recordDdelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID id = getIdParameterNotNull(request, "id");
		Application.getCommonService().delete(id);
		
		Map<String, Object> map = new HashMap<>();
		map.put("id", id);
		writeSuccess(response, map);
	}
	
	@RequestMapping("record-get")
	public void recordGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID id = getIdParameterNotNull(request, "id");
		Entity entity = EntityHelper.getEntity(id.getEntityCode());
		
		String fields = getParameter(request, "fields");
		if (StringUtils.isBlank(fields)) {
			fields = "";
			for (Field field : entity.getFields()) {
				fields += field.getName() + ',';
			}
		} else {
			for (String field : fields.split(",")) {
				if (StringHelper.isIdentifier(field)) {
					fields += field + ',';
				} else {
					LOG.warn("忽略无效/非法字段: " + field);
				}
			}
		}
		fields = fields.substring(0, fields.length() - 1);
		
		StringBuffer sql = new StringBuffer("select ")
				.append(fields)
				.append(" from ").append(entity.getName())
				.append(" where ").append(entity.getPrimaryField().getName()).append('=').append(id);
		Record record = Application.createQuery(sql.toString()).record();
		
		Map<String, Object> map = new HashMap<>();
		for (Iterator<String> iter = record.getAvailableFieldIterator(); iter.hasNext(); ) {
			String field = iter.next();
			Object value = record.getObjectValue(field);
			map.put(field, value);
		}
		map.put(entity.getPrimaryField().getName(), id);
		writeSuccess(response, map);
	}
}
