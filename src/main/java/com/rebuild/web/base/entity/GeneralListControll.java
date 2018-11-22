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
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.helper.manager.DataListManager;
import com.rebuild.server.helper.manager.LayoutManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.web.BaseControll;
import com.rebuild.web.LayoutConfig;
import com.rebuild.web.base.entity.datalist.DataListControl;
import com.rebuild.web.base.entity.datalist.DefaultDataListControl;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 数据列表
 * 
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
@Controller
@RequestMapping("/app/{entity}/")
public class GeneralListControll extends BaseControll implements LayoutConfig {

	@RequestMapping("list")
	public ModelAndView pageList(@PathVariable String entity, HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		Entity thatEntity = MetadataHelper.getEntity(entity);
		
		ModelAndView mv = null;
		if (thatEntity.getMasterEntity() != null) {
			mv = createModelAndView("/general-entity/slave-list.jsp", entity, user);
		} else {
			mv = createModelAndView("/general-entity/record-list.jsp", entity, user);
		}
		
		JSON config = DataListManager.getColumnLayout(entity, getRequestUser(request));
		mv.getModel().put("DataListConfig", JSON.toJSONString(config));
		
		return mv;
	}
	
	@RequestMapping("data-list")
	public void dataList(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSONObject query = (JSONObject) ServletUtils.getRequestJson(request);
		DataListControl control = new DefaultDataListControl(query, getRequestUser(request));
		JSON result = control.getResult();
		writeSuccess(response, result);
	}
	
	// --
	
	@RequestMapping(value = "list-fields", method = RequestMethod.POST)
	@Override
	public void sets(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		boolean toAll = "true".equals(getParameter(request, "toAll"));
		// 非管理员只能设置自己
		boolean isAdmin = Application.getUserStore().getUser(user).isAdmin();
		if (!isAdmin) {
			toAll = false;
		}
		
		JSON config = ServletUtils.getRequestJson(request);
		ID cfgid = getIdParameter(request, "cfgid");
		ID cfgidDetected = DataListManager.detectConfigId(cfgid, toAll, entity, DataListManager.TYPE_DATALIST, user);
		
		Record record = null;
		if (cfgidDetected == null) {
			record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
			record.setString("belongEntity", entityMeta.getName());
			record.setString("type", LayoutManager.TYPE_DATALIST);
			record.setString("applyTo", toAll ? LayoutManager.APPLY_ALL : LayoutManager.APPLY_SELF);
		} else {
			record = EntityHelper.forUpdate(cfgidDetected, user);
		}
		record.setString("config", config.toJSONString());
		Application.getCommonService().createOrUpdate(record);
		
		writeSuccess(response);
	}
	
	@RequestMapping(value = "list-fields", method = RequestMethod.GET)
	@Override
	public void gets(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		List<Map<String, Object>> fieldList = new ArrayList<>();
		for (Field field : MetadataSorter.sortFields(entityMeta)) {
			if (MetadataSorter.isFilterField(field)) {
				continue;
			}
			fieldList.add(DataListManager.formattedColumn(field));
		}
		
		List<Map<String, Object>> configList = new ArrayList<>();
		Object[] raw = DataListManager.getLayoutConfigRaw(entity, DataListManager.TYPE_DATALIST, user);
		if (raw != null) {
			for (Object o : (JSONArray) raw[1]) {
				JSONObject col = (JSONObject) o;
				String field = col.getString("field");
				if (entityMeta.containsField(field)) {
					configList.add(DataListManager.formattedColumn(entityMeta.getField(field)));
				} else {
					LOG.warn("Unknow field '" + field + "' in '" + entity + "'");
				}
			}
		}
		
		Map<String, Object> ret = new HashMap<>();
		ret.put("fieldList", fieldList);
		ret.put("configList", configList);
		if (raw != null) {
			ret.put("configId", raw[0].toString());
		}
		writeSuccess(response, ret);
	}
}
