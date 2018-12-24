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

package com.rebuild.server.helper.manager;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 首页仪表盘
 * 
 * @author devezhao
 * @since 12/20/2018
 */
public class DashboardManager extends ApplyFor {
	
	private static final Log LOG = LogFactory.getLog(DashboardManager.class);
	
	/**
	 * 获取可用面板
	 * 
	 * @param user
	 * @return
	 */
	public static JSON getDashList(ID user) {
		Object[][] array = Application.createQueryNoFilter(
				"select dashboardId,title,config,createdBy,shareTo from DashboardConfig where createdBy = ? or shareTo = 'ALL'")
				.setParameter(1, user)
				.array();
		
		// 没有就初始化一个
		if (array.length == 0) {
			Record record = EntityHelper.forNew(EntityHelper.DashboardConfig, user);
			String dname = "默认仪表盘";
			record.setString("title", dname);
			record.setString("config", JSONUtils.EMPTY_ARRAY_STR);
			record = Application.getCommonService().create(record);
			array = new Object[][] { new Object[] { record.getPrimary(), dname, JSONUtils.EMPTY_ARRAY_STR, user, "SELF" } };
		}
		
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
						"select title,type from ChartConfig where chartId = ?")
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
			array[i][0] = array[i][0].toString();
			array[i][3] = array[i][3].equals(user);  // 本人的
		}
		
		return (JSON) JSON.toJSON(array);
	}
	
	/**
	 * 获取可用图表
	 * 
	 * @param user
	 * @return
	 */
	public static JSON getChartList(ID user) {
		Object[][] array = Application.createQueryNoFilter(
				"select chartId,belongEntity,type,title,modifiedOn from ChartConfig where createdBy = ?")
				.setParameter(1, user)
				.array();
		for (Object[] chart : array) {
			String belongEntity = (String) chart[1];
			if (!MetadataHelper.containsEntity(belongEntity)) {
				LOG.warn("No entity found : " + belongEntity);
				continue;
			}
			
			chart[0] = chart[0].toString();
			chart[1] = EasyMeta.getLabel(MetadataHelper.getEntity(belongEntity));
			chart[4] = CalendarUtils.getUTCDateTimeFormat().format(chart[4]);
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
				"select createdBy from DashboardConfig where dashboardId = ?")
				.setParameter(1, dashid)
				.unique();
		return user.equals(dash[0]);
	}
}
