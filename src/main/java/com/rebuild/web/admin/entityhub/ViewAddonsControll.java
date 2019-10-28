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
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.portals.ViewAddonsManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.configuration.LayoutConfigService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import com.rebuild.web.PortalsConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * 视图-相关项显示
 * 
 * @author devezhao
 * @since 10/23/2018
 */
@Controller
@RequestMapping("/admin/entity/")
public class ViewAddonsControll extends BaseControll implements PortalsConfiguration {

	@RequestMapping(value = "{entity}/view-addons", method = RequestMethod.POST)
	@Override
	public void sets(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		String applyType = getParameter(request, "type", ViewAddonsManager.TYPE_TAB);
		JSON config = ServletUtils.getRequestJson(request);
		
		ID configId = ViewAddonsManager.instance.detectUseConfig(user, entity, applyType);
		Record record = null;
		if (configId == null) {
			record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
			record.setString("belongEntity", entity);
			record.setString("applyType", applyType);
			record.setString("shareTo", ViewAddonsManager.SHARE_ALL);
		} else {
			record = EntityHelper.forUpdate(configId, user);
		}
		record.setString("config", config.toJSONString());
		Application.getBean(LayoutConfigService.class).createOrUpdate(record);
		
		writeSuccess(response);
	}
	
	@RequestMapping(value = "{entity}/view-addons", method = RequestMethod.GET)
	@Override
	public void gets(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		String applyType = getParameter(request, "type", ViewAddonsManager.TYPE_TAB);

		Entity entityMeta = MetadataHelper.getEntity(entity);
		ConfigEntry config = ViewAddonsManager.instance.getLayout(user, entity, applyType);
		
		Set<String[]> refs = new HashSet<>();
		for (Field field : entityMeta.getReferenceToFields(true)) {
			Entity e = field.getOwnEntity();
			if (e.getMasterEntity() != null) {
				continue;
			}

			refs.add(new String[] { e.getName(), EasyMeta.getLabel(e) });
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "config", "refs" },
				new Object[] { config == null ? null : config.getJSON("config"), refs });
		writeSuccess(response, ret);
	}
}
