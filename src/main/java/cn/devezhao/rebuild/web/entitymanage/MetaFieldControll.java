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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EasyMeta;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.metadata.MetadataHelper;
import cn.devezhao.rebuild.server.service.entitymanage.DisplayType;
import cn.devezhao.rebuild.server.service.entitymanage.Field2Schema;
import cn.devezhao.rebuild.web.commons.BaseControll;
import cn.devezhao.rebuild.web.commons.PageForward;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/19/2018
 */
@Controller
@RequestMapping("/admin/entity/")
public class MetaFieldControll extends BaseControll  {
	
	@RequestMapping("{entity}/fields")
	public String pageEntityFields(@PathVariable String entity, HttpServletRequest request) throws IOException {
		MetaEntityControll.setEntityBase(request, entity);
		PageForward.setPageAttribute(request);
		return "/admin/entity/fields.jsp";
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
//			if (easyMeta.isBuiltin()) {
//				continue;
//			}
			
			Map<String, Object> map = new HashMap<>();
			map.put("fieldName", easyMeta.getName());
			map.put("fieldLabel", easyMeta.getLabel());
			map.put("displayType", easyMeta.getDisplayType());
			map.put("comments", easyMeta.getComments());
			map.put("createdOn", null);
			ret.add(map);
		}
		writeSuccess(response, ret);
	}

	@RequestMapping("{entity}/field/{field}")
	public String pageEntityFields(@PathVariable String entity, @PathVariable String field, HttpServletRequest request) throws IOException {
		EasyMeta easyMeta = MetaEntityControll.setEntityBase(request, entity);
		Field fieldMeta = ((Entity) easyMeta.getBaseMeta()).getField(field);
		System.out.println(fieldMeta);
		EasyMeta fieldEasyMeta = new EasyMeta(fieldMeta);
		
		request.setAttribute("fieldMetaId", fieldEasyMeta.isBuiltin() ? null : fieldEasyMeta.getMetaId());
		request.setAttribute("fieldName", fieldEasyMeta.getName());
		request.setAttribute("fieldLabel", fieldEasyMeta.getLabel());
		request.setAttribute("fieldComments", fieldEasyMeta.getComments());
		request.setAttribute("fieldType", fieldEasyMeta.getDisplayType());
		
		// 字段类型相关
		Type ft = fieldMeta.getType();
		if (ft == FieldType.DECIMAL) {
			request.setAttribute("fieldPrecision", fieldMeta.getDecimalScale());
		} else if (ft == FieldType.REFERENCE) {
			Entity refentity = fieldMeta.getReferenceEntities()[0];
			request.setAttribute("fieldRefentity", refentity.getName());
			request.setAttribute("fieldRefentityLabel", new EasyMeta(refentity).getLabel());
		}
		
		PageForward.setPageAttribute(request);
		return "/admin/entity/field-edit.jsp";
	}
	
	@RequestMapping("field-new")
	public void fieldNew(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		String entityName = getParameterNotNull(request, "entity");
		String label = getParameterNotNull(request, "label");
		String type = getParameterNotNull(request, "type");
		String comments = getParameter(request, "comments");
		String refEntity = getParameter(request, "refEntity");
		
		Entity entity = EntityHelper.getEntity(entityName);
		DisplayType dt = DisplayType.valueOf(type);
		
		String fieldName = new Field2Schema(user).create(entity, label, dt, comments, refEntity);
		if (fieldName != null) {
			writeSuccess(response, fieldName);
		} else {
			writeFailure(response);
		}
	}
	
	@RequestMapping("field-update")
	public void fieldUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);
		Record record = EntityHelper.parse((JSONObject) formJson, user);
		Application.getCommonService().update(record);
		
		MetadataHelper.refreshMetadata();
		writeSuccess(response);
	}
}
