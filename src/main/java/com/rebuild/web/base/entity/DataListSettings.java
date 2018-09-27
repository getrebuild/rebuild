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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.base.DataListManager;
import com.rebuild.server.service.base.LayoutManager;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;

/**
 * 数据列表相关配置
 * 
 * @author zhaofang123@gmail.com
 * @since 09/15/2018
 */
@Controller
@RequestMapping("/app/")
public class DataListSettings extends BaseControll {

	@RequestMapping(value = "{entity}/list-columns", method = RequestMethod.POST)
	public void columnsSet(@PathVariable String entity, HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		JSON config = ServletUtils.getRequestJson(request);
		ID configId = getIdParameter(request, "cfgid");
		
		Record record = null;
		if (configId == null) {
			record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
			record.setString("belongEntity", entityMeta.getName());
			record.setString("type", LayoutManager.TYPE_DATALIST);
		} else {
			record = EntityHelper.forUpdate(configId, user);
		}
		record.setString("config", config.toJSONString());
		Application.getCommonService().createOrUpdate(record);
		writeSuccess(response);
	}
	
	@RequestMapping(value = "{entity}/list-columns", method = RequestMethod.GET)
	public void columnsGet(@PathVariable String entity, HttpServletRequest request, HttpServletResponse response) throws IOException {
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		List<Map<String, Object>> fieldList = new ArrayList<>();
		for (Field field : entityMeta.getFields()) {
			if (field.getType() == FieldType.PRIMARY) {
				continue;
			}
			fieldList.add(DataListManager.warpColumn(field));
		}
		
		List<Map<String, Object>> configList = new ArrayList<>();
		Object[] lcr = DataListManager.getLayoutConfigRaw(entity, DataListManager.TYPE_DATALIST);
		if (lcr != null) {
			for (Object o : (JSONArray) lcr[1]) {
				JSONObject jo = (JSONObject) o;
				String field = jo.getString("field");
				if (entityMeta.containsField(field)) {
					configList.add(DataListManager.warpColumn(entityMeta.getField(field)));
				} else {
					LOG.warn("Invalid field : " + field);
				}
			}
		}
		
		Map<String, Object> ret = new HashMap<>();
		ret.put("fieldList", fieldList);
		ret.put("configList", configList);
		if (lcr != null) {
			ret.put("configId", lcr[0].toString());
		}
		writeSuccess(response, ret);
	}
}
