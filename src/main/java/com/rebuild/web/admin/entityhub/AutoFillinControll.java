/*
rebuild - Building your business-systems freely.
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

package com.rebuild.web.admin.entityhub;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.configuration.AutoFillinConfigService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import com.rebuild.web.PortalsConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/17
 */
@Controller
@RequestMapping("/admin/entity/{entity}/field/")
public class AutoFillinControll extends BasePageControll implements PortalsConfiguration {
	
	@RequestMapping("{field}/auto-fillin")
	public ModelAndView page(@PathVariable String entity, @PathVariable String field,
			HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/entityhub/auto-fillin.jsp");
		EasyMeta easyMeta = MetaEntityControll.setEntityBase(mv, entity);
		
		Field fieldMeta = ((Entity) easyMeta.getBaseMeta()).getField(field);
		EasyMeta fieldEasyMeta = new EasyMeta(fieldMeta);
		mv.getModel().put("fieldName", fieldEasyMeta.getName());
		mv.getModel().put("referenceEntity", fieldMeta.getReferenceEntity().getName());
		mv.getModel().put("referenceEntityLabel", EasyMeta.getLabel(fieldMeta.getReferenceEntity()));
		return mv;
	}

	@RequestMapping("auto-fillin-save")
	@Override
	public void sets(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);
		final String field = data.getString("field");
		
		Record record = null;
		if (ID.isId(data.getString("id"))) {
			record = EntityHelper.forUpdate(ID.valueOf(data.getString("id")), user);
		} else {
			record = EntityHelper.forNew(EntityHelper.AutoFillinConfig, user);
			record.setString("belongEntity", entity);
			record.setString("belongField", field);
		}
		
		if (data.containsKey("sourceField")) {
			record.setString("sourceField", data.getString("sourceField"));
		}
		if (data.containsKey("targetField")) {
			String targetField = data.getString("targetField");
			Object[] exists = Application.createQuery(
					"select configId from AutoFillinConfig where belongEntity = ? and belongField = ? and targetField = ?")
					.setParameter(1, entity)
					.setParameter(2, field)
					.setParameter(3, targetField)
					.unique();
			if (exists != null) {
				if (record.getPrimary() == null || !exists[0].equals(record.getPrimary())) {
					writeFailure(response, "目标字段重复");
					return;
				}
			}
			
			record.setString("targetField", targetField);
		}
		record.setString("extConfig", data.getString("extConfig"));
		
		Application.getBean(AutoFillinConfigService.class).createOrUpdate(record);
		writeSuccess(response);
	}
	
	@RequestMapping("auto-fillin-list")
	@Override
	public void gets(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		String belongField = getParameterNotNull(request, "field");
		Field field = MetadataHelper.getField(entity, belongField);
		Entity sourceEntity = field.getReferenceEntity();
		Entity targetEntity = field.getOwnEntity();
		
		Object[][] array = Application.createQueryNoFilter(
				"select configId,sourceField,targetField,extConfig from AutoFillinConfig where belongEntity = ? and belongField = ? order by modifiedOn desc")
				.setParameter(1, entity)
				.setParameter(2, belongField)
				.array();
		
		JSONArray rules = new JSONArray();
		for (Object[] o : array) {
			String sourceField = (String) o[1];
			String targetField = (String) o[2];
			if (!MetadataHelper.checkAndWarnField(sourceEntity, sourceField)
					|| !MetadataHelper.checkAndWarnField(targetEntity, targetField)) {
				continue;
			}

			JSON rule = JSONUtils.toJSONObject(
					new String[] { "id", "sourceField", "sourceFieldLabel", "targetField", "targetFieldLabel", "extConfig" }, 
					new Object[] { o[0], 
							sourceField, EasyMeta.getLabel(sourceEntity.getField(sourceField)),
							targetField, EasyMeta.getLabel(targetEntity.getField(targetField)),
							JSON.parse((String) o[3]) });
			rules.add(rule);
		}
		writeSuccess(response, rules);
	}
}
