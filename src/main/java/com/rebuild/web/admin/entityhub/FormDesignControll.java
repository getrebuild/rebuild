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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.portals.FormsManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.configuration.LayoutConfigService;
import com.rebuild.web.BasePageControll;
import com.rebuild.web.PortalsConfiguration;
import org.apache.commons.lang.StringUtils;
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
public class FormDesignControll extends BasePageControll implements PortalsConfiguration {
	
	@RequestMapping("{entity}/form-design")
	public ModelAndView pageFormDesign(@PathVariable String entity, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/entityhub/form-design.jsp");
		MetaEntityControll.setEntityBase(mv, entity);
		ConfigEntry config = FormsManager.instance.getFormLayout(entity, getRequestUser(request));
		if (config != null) {
			request.setAttribute("FormConfig", config.toJSON());
		}
		return mv;
	}
	
	@Override
	public void gets(String entity, HttpServletRequest request, HttpServletResponse response) throws IOException { }
	
	@RequestMapping({ "{entity}/form-update" })
	@Override
	public void sets(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSON formJson = ServletUtils.getRequestJson(request);

		// 修改字段名称
		Map<String, String> newLabels = new HashMap<>();
		JSONArray config = ((JSONObject) formJson).getJSONArray("config");
		for (Object o : config) {
			JSONObject item = (JSONObject) o;
			String newLabel = item.getString("__newLabel");
			if (StringUtils.isNotBlank(newLabel)) {
				newLabels.put(item.getString("field"), newLabel);
			}
			item.remove("__newLabel");
		}
		((JSONObject) formJson).put("config", config);

		Record record = EntityHelper.parse((JSONObject) formJson, getRequestUser(request));
		if (record.getPrimary() == null) {
			record.setString("shareTo", FormsManager.SHARE_ALL);
		}
		record = Application.getBean(LayoutConfigService.class).createOrUpdate(record);

		if (!newLabels.isEmpty()) {
			List<Record> willUpdate = new ArrayList<>();
			Entity entityMeta = MetadataHelper.getEntity(entity);
			for (Map.Entry<String, String> e : newLabels.entrySet()) {
				Field fieldMeta = entityMeta.getField(e.getKey());
				EasyMeta fieldEasy = EasyMeta.valueOf(fieldMeta);
				if (fieldEasy.isBuiltin() || fieldEasy.getMetaId() == null) {
					continue;
				}

				Record fieldRecord = EntityHelper.forUpdate(fieldEasy.getMetaId(), UserService.SYSTEM_USER, false);
				fieldRecord.setString("fieldLabel", e.getValue());
				willUpdate.add(fieldRecord);
			}

			if (!willUpdate.isEmpty()) {
				Application.getCommonService().createOrUpdate(willUpdate.toArray(new Record[0]), false);
				MetadataHelper.getMetadataFactory().refresh(false);
			}
		}

		writeSuccess(response);
	}
}
