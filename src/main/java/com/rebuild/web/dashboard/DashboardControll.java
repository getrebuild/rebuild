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

package com.rebuild.web.dashboard;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardControll extends BaseControll {

	@RequestMapping("/home")
	public ModelAndView pageHome(HttpServletRequest request) {
		return createModelAndView("/dashboard/home.jsp");
	}
	
	@RequestMapping("/dash-gets")
	public void dashGets(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		Object[][] array = Application.createQueryNoFilter(
				"select dashboardId,title,config from DashboardConfig where createdBy = ?")
				.setParameter(1, user)
				.array();
		
		// 没有就初始化一个
		if (array.length == 0) {
			Record record = EntityHelper.forNew(EntityHelper.DashboardConfig, user);
			String dname = "默认仪表盘";
			record.setString("title", dname);
			record.setString("config", "[]");
			record = Application.getCommonService().create(record);
			array = new Object[][] { new Object[] { record.getPrimary(), dname, null } };
		} else {
			// 补充标题
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
					item.put("title", chart[0]);
					item.put("type", chart[1]);
				}
				array[i][2] = config;
			}
		}
		
		writeSuccess(response, array);
	}
	
	@RequestMapping("/dash-save")
	public void dashSave(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID dashid = getIdParameterNotNull(request, "id");
		ID user = getRequestUser(request);
		JSON config = ServletUtils.getRequestJson(request);
		
		Record record = EntityHelper.forUpdate(dashid, user);
		record.setString("config", config.toJSONString());
		Application.getCommonService().update(record);
		writeSuccess(response);
	}
}
