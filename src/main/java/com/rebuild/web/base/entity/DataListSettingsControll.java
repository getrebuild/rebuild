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
import com.rebuild.server.configuration.portals.BaseLayoutManager;
import com.rebuild.server.configuration.portals.DataListManager;
import com.rebuild.server.configuration.portals.SharableManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.portals.LayoutConfigService;
import com.rebuild.web.BaseControll;
import com.rebuild.web.PortalsConfiguration;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 列表配置
 * 
 * @author devezhao
 * @since 01/07/2019
 */
@Controller
@RequestMapping("/app/{entity}/")
public class DataListSettingsControll extends BaseControll implements PortalsConfiguration {

	@RequestMapping(value = "list-fields", method = RequestMethod.POST)
	@Override
	public void sets(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		boolean toAll = getBoolParameter(request, "toAll", false);
		if (toAll) {
			toAll = UserHelper.isAdmin(user);
		}
		
		if (!MetadataHelper.containsEntity(entity)) {
			writeFailure(response);
			return;
		}
		
		JSON config = ServletUtils.getRequestJson(request);
		ID cfgid = getIdParameter(request, "cfgid");
		if (cfgid != null && !SharableManager.isSelf(user, cfgid)) {
			cfgid = null;
		}
		
		Record record = null;
		if (cfgid == null) {
			record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
			record.setString("belongEntity", entity);
			record.setString("applyType", BaseLayoutManager.TYPE_DATALIST);
		} else {
			record = EntityHelper.forUpdate(cfgid, user);
		}
		record.setString("shareTo", toAll ? SharableManager.SHARE_ALL : SharableManager.SHARE_SELF);
		record.setString("config", config.toJSONString());
		Application.getBean(LayoutConfigService.class).createOrUpdate(record);
		
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
			fieldList.add(DataListManager.formattedColumn(field));
		}
		// 引用实体的字段
		for (Field field : MetadataSorter.sortFields(entityMeta, DisplayType.REFERENCE)) {
			// 过滤所属用户/所属部门等系统字段
			if (EasyMeta.valueOf(field).isBuiltin()) {
				continue;
			}
			Entity refEntity = field.getReferenceEntity();
			for (Field field4Ref : MetadataSorter.sortFields(refEntity)) {
				fieldList.add(DataListManager.formattedColumn(field4Ref, field));
			}
		}
		
		List<Map<String, Object>> configList = new ArrayList<>();
		Object[] raw = DataListManager.getLayoutOfDatalist(user, entity);
		if (raw != null) {
			for (Object o : (JSONArray) raw[1]) {
				JSONObject col = (JSONObject) o;
				String field = col.getString("field");
				String fieldPaths[] = field.split("\\.");
				if (!entityMeta.containsField(fieldPaths[0])) {
					LOG.warn("Unknow field '" + field + "' in '" + entity + "'");
					continue;
				}
				
				Field fieldMeta = entityMeta.getField(fieldPaths[0]);
				if (fieldPaths.length == 1) {
					configList.add(DataListManager.formattedColumn(fieldMeta));
				} else {
					Entity refEntity = fieldMeta.getReferenceEntity();
					if (refEntity != null && refEntity.containsField(fieldPaths[1])) {
						configList.add(DataListManager.formattedColumn(refEntity.getField(fieldPaths[1]), fieldMeta));
					} else {
						LOG.warn("Unknow field '" + field + "' in '" + entity + "'");
					}
				}
			}
		}
		
		Map<String, Object> ret = new HashMap<>();
		ret.put("fieldList", fieldList);
		ret.put("configList", configList);
		if (raw != null) {
			ret.put("configId", raw[0]);
			ret.put("shareTo", raw[2]);
		}
		writeSuccess(response, ret);
	}
}
