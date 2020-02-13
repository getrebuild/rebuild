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
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.BaseLayoutManager;
import com.rebuild.server.configuration.portals.NavManager;
import com.rebuild.server.configuration.portals.ShareToManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.RoleService;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.privileges.ZeroEntry;
import com.rebuild.server.service.configuration.LayoutConfigService;
import com.rebuild.web.BaseControll;
import com.rebuild.web.PortalsConfiguration;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 导航菜单设置
 * 
 * @author zhaofang123@gmail.com
 * @since 09/19/2018
 */
@Controller
@RequestMapping("/app/settings/")
public class NavSettings extends BaseControll implements PortalsConfiguration {
	
	@Override
	public void sets(String entity, HttpServletRequest request, HttpServletResponse response) { }
	@Override
	public void gets(String entity, HttpServletRequest request, HttpServletResponse response) { }
	
	@RequestMapping(value = "nav-settings", method = RequestMethod.POST)
	public void sets(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final ID user = getRequestUser(request);
		Assert.isTrue(Application.getSecurityManager().allow(user, ZeroEntry.AllowCustomNav), "没有权限");

		ID cfgid = getIdParameter(request, "id");
		// 普通用户只能有一个
		if (cfgid != null && !ShareToManager.isSelf(user, cfgid)) {
			ID useNav = NavManager.instance.detectUseConfig(user, null, NavManager.TYPE_NAV);
			if (useNav != null && ShareToManager.isSelf(user, useNav)) {
				cfgid = useNav;
			} else {
				cfgid = null;
			}
		}

		JSON config = ServletUtils.getRequestJson(request);

		Record record;
		if (cfgid == null) {
			record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
			record.setString("belongEntity", "N");
			record.setString("applyType", BaseLayoutManager.TYPE_NAV);
			record.setString("shareTo", BaseLayoutManager.SHARE_SELF);
		} else {
			record = EntityHelper.forUpdate(cfgid, user);
		}
		record.setString("config", config.toJSONString());
		putCommonsFields(request, record);
		Application.getBean(LayoutConfigService.class).createOrUpdate(record);

		writeSuccess(response);
	}
	
	@RequestMapping(value = "nav-settings", method = RequestMethod.GET)
	public void gets(HttpServletRequest request, HttpServletResponse response) {
		final ID user = getRequestUser(request);
		final String cfgid = request.getParameter("id");

		// 管理员新建
		if ("NEW".equalsIgnoreCase(cfgid)) {
			writeSuccess(response);
		} else if (ID.isId(cfgid)) {
			writeSuccess(response, NavManager.instance.getNavLayoutById(ID.valueOf(cfgid)));
		} else {
			writeSuccess(response, NavManager.instance.getNavLayout(user));
		}
	}

	@RequestMapping(value = "nav-settings/alist", method = RequestMethod.GET)
	public void getsList(HttpServletRequest request, HttpServletResponse response) {
		final ID user = getRequestUser(request);

		String sql = "select configId,configName,shareTo,createdBy from LayoutConfig where ";
		if (UserHelper.isAdmin(user)) {
			sql += String.format("applyType = '%s' and createdBy.roleId = '%s' order by configName",
					NavManager.TYPE_NAV, RoleService.ADMIN_ROLE);
		} else {
			// 普通用户可用的
			ID[] uses = NavManager.instance.getUsesNavId(user);
			sql += "configId in ('" + StringUtils.join(uses, "', '") + "')";
		}

		Object[][] list = Application.createQueryNoFilter(sql).array();
		writeSuccess(response, list);
	}
}
