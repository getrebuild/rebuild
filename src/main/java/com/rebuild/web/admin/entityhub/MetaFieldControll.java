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

package com.rebuild.web.admin.entityhub;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.FieldPortalAttrs;
import com.rebuild.server.helper.state.StateHelper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.metadata.entity.Field2Schema;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/19/2018
 */
@Controller
@RequestMapping("/admin/entity/")
public class MetaFieldControll extends BasePageControll  {
	
	@RequestMapping("{entity}/fields")
	public ModelAndView pageEntityFields(@PathVariable String entity, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/entityhub/fields.jsp");
		MetaEntityControll.setEntityBase(mv, entity);
		String nameField = MetadataHelper.getNameField(entity).getName();
		mv.getModel().put("nameField", nameField);
		mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
		return mv;
	}
	
	@RequestMapping("list-field")
	public void listField(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entityName = getParameter(request, "entity");
		Entity entity = MetadataHelper.getEntity(entityName);
		String fromType = getParameter(request, "from");

		List<Map<String, Object>> ret = new ArrayList<>();
		for (Field field : MetadataSorter.sortFields(entity)) {
			if (!FieldPortalAttrs.instance.allowByType(field, fromType)) {
				continue;
			}

			EasyMeta easyMeta = new EasyMeta(field);
			Map<String, Object> map = new HashMap<>();
			if (easyMeta.getMetaId() != null) {
				map.put("fieldId", easyMeta.getMetaId());
			}
			map.put("fieldName", easyMeta.getName());
			map.put("fieldLabel", easyMeta.getLabel());
			map.put("comments", easyMeta.getComments());
			map.put("displayType", easyMeta.getDisplayType().getDisplayName());
			map.put("nullable", field.isNullable());
			map.put("builtin", easyMeta.isBuiltin());
			map.put("creatable", field.isCreatable());
			ret.add(map);
		}
		writeSuccess(response, ret);
	}

	@RequestMapping("{entity}/field/{field}")
	public ModelAndView pageEntityField(@PathVariable String entity, @PathVariable String field, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/entityhub/field-edit.jsp");
		EasyMeta easyMeta = MetaEntityControll.setEntityBase(mv, entity);
		
		Field fieldMeta = ((Entity) easyMeta.getBaseMeta()).getField(field);
		EasyMeta fieldEasyMeta = new EasyMeta(fieldMeta);
		
		mv.getModel().put("fieldMetaId", fieldEasyMeta.getMetaId());
		mv.getModel().put("fieldName", fieldEasyMeta.getName());
		mv.getModel().put("fieldLabel", fieldEasyMeta.getLabel());
		mv.getModel().put("fieldComments", fieldEasyMeta.getComments());
		mv.getModel().put("fieldType", fieldEasyMeta.getDisplayType(false));
		mv.getModel().put("fieldTypeLabel", fieldEasyMeta.getDisplayType(true));
		mv.getModel().put("fieldNullable", fieldMeta.isNullable());
		mv.getModel().put("fieldUpdatable", fieldMeta.isUpdatable());
		mv.getModel().put("fieldRepeatable", fieldMeta.isRepeatable());
		mv.getModel().put("fieldBuildin", fieldEasyMeta.isBuiltin());
		mv.getModel().put("fieldDefaultValue", fieldMeta.getDefaultValue());
		mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
		
		// 字段类型相关
		Type ft = fieldMeta.getType();
		if (ft == FieldType.REFERENCE) {
			Entity refentity = fieldMeta.getReferenceEntities()[0];
			mv.getModel().put("fieldRefentity", refentity.getName());
			mv.getModel().put("fieldRefentityLabel", new EasyMeta(refentity).getLabel());
		}
		mv.getModel().put("fieldExtConfig", fieldEasyMeta.getFieldExtConfig());
		
		return mv;
	}
	
	@RequestMapping("field-new")
	public void fieldNew(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSONObject reqJson = (JSONObject) ServletUtils.getRequestJson(request);

		String entityName = reqJson.getString("entity");
		String label = reqJson.getString("label");
		String type = reqJson.getString("type");
		String comments = reqJson.getString("comments");
		String refEntity = reqJson.getString("refEntity");
		String refClassification = reqJson.getString("refClassification");
		String stateClass = reqJson.getString("stateClass");

		Entity entity = MetadataHelper.getEntity(entityName);
		DisplayType dt = DisplayType.valueOf(type);
		
		JSON extConfig = null;
		if (dt == DisplayType.CLASSIFICATION) {
			ID dataId = ID.valueOf(refClassification);
			extConfig = JSONUtils.toJSONObject("classification", dataId);
		} else if (dt == DisplayType.STATE) {
		    if (!StateHelper.isStateClass(stateClass)) {
                writeFailure(response, "无效状态类");
                return;
            }
            extConfig = JSONUtils.toJSONObject("stateClass", stateClass);
        }
		
		String fieldName;
		try {
			fieldName = new Field2Schema(user).createField(entity, label, dt, comments, refEntity, extConfig);
			writeSuccess(response, fieldName);
		} catch (Exception ex) {
			writeFailure(response, ex.getLocalizedMessage());
		}
	}
	
	@RequestMapping("field-update")
	public void fieldUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);
		Record record = EntityHelper.parse((JSONObject) formJson, user);
		Application.getCommonService().update(record);
		
		Application.getMetadataFactory().refresh(false);
		writeSuccess(response);
	}
	
	@RequestMapping("field-drop")
	public void fieldDrop(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID fieldId = getIdParameterNotNull(request, "id");
		
		Object[] fieldRecord = Application.createQueryNoFilter(
				"select belongEntity,fieldName from MetaField where fieldId = ?")
				.setParameter(1, fieldId)
				.unique();
		Field field = MetadataHelper.getEntity((String) fieldRecord[0]).getField((String) fieldRecord[1]);
		
		boolean drop = new Field2Schema(user).dropField(field, false);
		if (drop) {
			writeSuccess(response);
		} else {
			writeFailure(response, "删除失败，请确认该字段是否可删除");
		}
	}
}
