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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.helper.manager.ViewAddonsManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import com.rebuild.web.PortalsConfiguration;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

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
		
		Object[] addons = ViewAddonsManager.getConfig(entity, applyType);
		
		Record record = null;
		if (addons == null) {
			record = EntityHelper.forNew(EntityHelper.ViewAddonsConfig, user);
			record.setString("belongEntity", entity);
			record.setString("applyType", applyType);
		} else {
			record = EntityHelper.forUpdate((ID) addons[0], user);
		}
		record.setString("config", config.toJSONString());
		Application.getCommonService().createOrUpdate(record);
		
		writeSuccess(response);
	}
	
	@RequestMapping(value = "{entity}/view-addons", method = RequestMethod.GET)
	@Override
	public void gets(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		String applyType = getParameter(request, "type", ViewAddonsManager.TYPE_TAB);
		
		Entity entityMeta = MetadataHelper.getEntity(entity);
		Object[] addons = ViewAddonsManager.getConfig(entity, applyType);
		
		Set<String[]> refs = new HashSet<>();
		for (Field field : entityMeta.getReferenceToFields()) {
			Entity e = field.getOwnEntity();
			// 过滤明细实体，因为明细实体默认就有
			if (e.getMasterEntity() != null) {
				continue;
			}
			refs.add(new String[] { e.getName(), EasyMeta.getLabel(e) });
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "config", "refs" },
				new Object[] { addons == null ? null : addons[1], refs });
		writeSuccess(response, ret);
	}
}
