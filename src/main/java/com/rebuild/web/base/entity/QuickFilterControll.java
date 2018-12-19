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

package com.rebuild.web.base.entity;

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
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.helper.manager.DataListManager;
import com.rebuild.server.helper.manager.LayoutManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.service.query.AdvFilterManager;
import com.rebuild.web.BaseControll;
import com.rebuild.web.LayoutConfig;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 快速查询
 * 
 * @author devezhao
 * @since 09/30/2018
 */
@Controller
@RequestMapping("/app/{entity}/")
public class QuickFilterControll extends BaseControll implements LayoutConfig {
	
	@RequestMapping(value="advfilter/quick-fields", method = RequestMethod.POST)
	@Override
	public void sets(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		
		boolean toAll = getBoolParameter(request, "toAll");
		// 非管理员只能设置自己
		if (toAll) {
			toAll = Application.getUserStore().getUser(user).isAdmin();
		}
		
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		JSON config = ServletUtils.getRequestJson(request);
		ID cfgid = getIdParameter(request, "cfgid");
		ID cfgidDetected = AdvFilterManager.detectQuickConfigId(cfgid, toAll, entityMeta.getName(), user);
		
		Record record = null;
		if (cfgidDetected == null) {
			record = EntityHelper.forNew(EntityHelper.FilterConfig, user);
			record.setString("belongEntity", entityMeta.getName());
			record.setString("filterName", AdvFilterManager.FILTER_QUICK);
			record.setString("applyTo", toAll ? LayoutManager.APPLY_ALL : LayoutManager.APPLY_SELF);
		} else {
			record = EntityHelper.forUpdate(cfgidDetected, user);
		}
		record.setString("config", config.toJSONString());
		Application.getCommonService().createOrUpdate(record);
		
		writeSuccess(response);
	}
	
	@RequestMapping(value="advfilter/quick-fields", method = RequestMethod.GET)
	@Override
	public void gets(@PathVariable String entity, 
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		List<Map<String, Object>> fieldList = new ArrayList<>();
		for (Field field : MetadataSorter.sortFields(entityMeta)) {
			if (AdvFilterManager.allowedQuickFilter(field)) {
				fieldList.add(DataListManager.formattedColumn(field));
			}
		}
		
		Object[] config = AdvFilterManager.getQuickFilterRaw(entity, user);
		Map<String, Object> ret = new HashMap<>();
		ret.put("fieldList", fieldList);
		if (config != null) {
			ret.put("config", config[1]);
			ret.put("configId", config[0] != null ? config[0].toString() : null);
		}
		writeSuccess(response, ret);
	}
	
	@RequestMapping(value="advfilter/quick-gets", method = RequestMethod.GET)
	public void getQuickFilter(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		Object[] config = AdvFilterManager.getQuickFilterRaw(entityMeta.getName(), user);
		if (config == null) {
			writeSuccess(response);
			return;
		}
		
		JSONObject quick = (JSONObject) config[1];
		writeSuccess(response, quick);
	}
}
