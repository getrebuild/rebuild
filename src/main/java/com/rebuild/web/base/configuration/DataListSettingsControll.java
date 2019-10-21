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

package com.rebuild.web.base.configuration;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.portals.BaseLayoutManager;
import com.rebuild.server.configuration.portals.DataListManager;
import com.rebuild.server.configuration.portals.FieldPortalAttrs;
import com.rebuild.server.configuration.portals.ShareToManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.RoleService;
import com.rebuild.server.service.bizz.privileges.ZeroEntry;
import com.rebuild.server.service.configuration.LayoutConfigService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import com.rebuild.web.PortalsConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		Assert.isTrue(Application.getSecurityManager().allowed(user, ZeroEntry.AllowCustomDataList), "没有权限");

		JSON config = ServletUtils.getRequestJson(request);
		ID cfgid = getIdParameter(request, "id");
		if (cfgid != null && !ShareToManager.isSelf(user, cfgid)) {
			cfgid = null;
		}
		
		Record record;
		if (cfgid == null) {
			record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
			record.setString("belongEntity", entity);
			record.setString("applyType", BaseLayoutManager.TYPE_DATALIST);
			record.setString("shareTo", BaseLayoutManager.SHARE_SELF);
		} else {
			record = EntityHelper.forUpdate(cfgid, user);
		}
		record.setString("config", config.toJSONString());
		putCommonsFields(request, record);
		Application.getBean(LayoutConfigService.class).createOrUpdate(record);
		
		writeSuccess(response);
	}
	
	@RequestMapping(value = "list-fields", method = RequestMethod.GET)
	@Override
	public void gets(@PathVariable String entity,
					 HttpServletRequest request, HttpServletResponse response) {
		ID user = getRequestUser(request);
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		List<Map<String, Object>> fieldList = new ArrayList<>();
		for (Field field : MetadataSorter.sortFields(entityMeta)) {
			if (FieldPortalAttrs.instance.allowDataList(field)) {
				fieldList.add(DataListManager.instance.formatField(field));
			}
		}
		// 引用实体的字段
		for (Field field : MetadataSorter.sortFields(entityMeta, DisplayType.REFERENCE)) {
			// 过滤所属用户/所属部门等系统字段
			if (EasyMeta.valueOf(field).isBuiltin()) {
				continue;
			}
			Entity refEntity = field.getReferenceEntity();
			// 无权限的不返回
			if (!Application.getSecurityManager().allowedR(user, refEntity.getEntityCode())) {
				continue;
			}
			
			for (Field fieldOfRef : MetadataSorter.sortFields(refEntity)) {
				if (FieldPortalAttrs.instance.allowDataList(fieldOfRef)) {
					fieldList.add(DataListManager.instance.formatField(fieldOfRef, field));
				}
			}
		}
		
		ConfigEntry raw = null;
		String cfgid = request.getParameter("id");
		if ("NEW".equalsIgnoreCase(cfgid)) {
			raw = new ConfigEntry();
			raw.set("config", JSONUtils.EMPTY_ARRAY);
		} else if (ID.isId(cfgid)) {
			raw = DataListManager.instance.getLayoutById(ID.valueOf(cfgid));
		} else {
			raw = DataListManager.instance.getLayoutOfDatalist(user, entity);
		}

		JSONObject config = (JSONObject) DataListManager.instance.formatFieldsLayout(entity, user, false, raw);

		Map<String, Object> ret = new HashMap<>();
		ret.put("fieldList", fieldList);
		ret.put("configList", config.getJSONArray("fields"));
		if (raw != null) {
			ret.put("configId", raw.getID("id"));
			ret.put("shareTo", raw.getString("shareTo"));
		}
		writeSuccess(response, ret);
	}

	@RequestMapping(value = "list-fields/alist", method = RequestMethod.GET)
	public void getsList(@PathVariable String entity,
						 HttpServletRequest request, HttpServletResponse response) {
		Object[][] list = Application.createQueryNoFilter(
				"select configId,configName,shareTo from LayoutConfig where belongEntity = ? and applyType = ? and createdBy.roleId = ? order by configName")
				.setParameter(1, entity)
				.setParameter(2, BaseLayoutManager.TYPE_DATALIST)
				.setParameter(3, RoleService.ADMIN_ROLE)
				.array();
		writeSuccess(response, list);
	}
}
