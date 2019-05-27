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

package com.rebuild.server.configuration.portals;

import java.util.Iterator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.RoleService;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 首页仪表盘
 * 
 * @author devezhao
 * @since 12/20/2018
 */
public class DashboardManager extends SharableManager {
	
	/**
	 * 获取可用面板
	 * 
	 * @param user
	 * @return
	 */
	public static JSON getDashList(ID user) {
		ID configHas = detectUseConfig(user, "DashboardConfig");
		// 没有就初始化一个
		if (configHas == null) {
			Record record = EntityHelper.forNew(EntityHelper.DashboardConfig, user);
			record.setString("config", JSONUtils.EMPTY_ARRAY_STR);
			record.setString("title", UserHelper.isAdmin(user) ? "默认仪表盘" : "我的仪表盘");
			record.setString("shareTo", UserHelper.isAdmin(user) ? SHARE_ALL : SHARE_SELF);
			record = Application.getCommonService().create(record);
			configHas = record.getPrimary();
		}
		
		String sql = "select configId,title,config,createdBy,shareTo from DashboardConfig where ";
		if (UserHelper.isAdmin(user)) {
			sql += String.format("createdBy.roleId = '%s'", RoleService.ADMIN_ROLE.toLiteral());
		} else {
			sql += String.format("createdBy = '%s' or shareTo = 'ALL'", user.toLiteral());
		}
		sql += " order by title asc";
		Object[][] array = Application.createQueryNoFilter(sql).array();
		
		// 补充图表标题
		for (int i = 0; i < array.length; i++) {
			JSONArray config = JSON.parseArray((String) array[i][2]);
			for (Iterator<Object> iter = config.iterator(); iter.hasNext(); ) {
				JSONObject item = (JSONObject) iter.next();
				String chartid = item.getString("chart");
				if (!ID.isId(chartid)) {
					iter.remove();
					continue;
				}
				
				Object[] chart = Application.createQueryNoFilter(
						"select title,chartType from ChartConfig where chartId = ?")
						.setParameter(1, ID.valueOf(chartid))
						.unique();
				if (chart == null) {
					iter.remove();
					continue;
				}
				
				item.put("title", chart[0]);
				item.put("type", chart[1]);
			}
			
			array[i][2] = config;
			array[i][3] = allowedUpdate(user, (ID) array[i][0]);
			array[i][0] = array[i][0].toString();
		}
		
		return (JSON) JSON.toJSON(array);
	}
	
	/**
	 * 是否允许修改
	 * 
	 * @param user
	 * @param dashid
	 * @return
	 */
	public static boolean allowedUpdate(ID user, ID dashid) {
		Object[] dash = Application.createQueryNoFilter(
				"select createdBy from DashboardConfig where configId = ?")
				.setParameter(1, dashid)
				.unique();
		if (dash == null) {
			return false;
		}
		
		if (UserHelper.isAdmin(user)) {
			return UserHelper.isAdmin((ID) dash[0]);
		} else {
			return user.equals(dash[0]);
		}
	}
}
