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

package com.rebuild.web.dashboard;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.momentjava.Moment;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.charts.ChartsFactory;
import com.rebuild.server.business.charts.builtin.BuiltinChart;
import com.rebuild.server.configuration.portals.DashboardManager;
import com.rebuild.server.configuration.portals.ShareToManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.RoleService;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.configuration.DashboardConfigService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardControll extends BasePageControll {

	@RequestMapping("/home")
	public ModelAndView pageHome(HttpServletRequest request) {
		return createModelAndView("/dashboard/home.jsp");
	}
	
	@RequestMapping("/dash-gets")
	public void dashGets(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON dashs = DashboardManager.instance.getDashList(user);
		writeSuccess(response, dashs);
	}
	
	@RequestMapping("/dash-new")
	public void dashNew(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSONObject formJson = (JSONObject) ServletUtils.getRequestJson(request);
		JSONArray dashCopy = formJson.getJSONArray("__copy");
		if (dashCopy != null) {
			formJson.remove("__copy");
		}
		formJson.put("config", JSONUtils.EMPTY_ARRAY_STR);
		
		Record dashRecord = EntityHelper.parse(formJson, user);
		
		// 复制当前面板
		if (dashCopy != null) {
			for (Object o : dashCopy) {
				JSONObject item = (JSONObject) o;
				Record chart = Application.createQueryNoFilter(
						"select config,belongEntity,chartType,title,createdBy from ChartConfig where chartId = ?")
						.setParameter(1, ID.valueOf(item.getString("chart")))
						.record();
				if (chart == null) {
					continue;
				}
				// 自己的直接使用
				if (ShareToManager.isSelf(user, chart.getID("createdBy"))) {
					continue;
				}

				chart.removeValue("createdBy");
				Record chartRecord = EntityHelper.forNew(EntityHelper.ChartConfig, user);
				for (Iterator<String> iter = chart.getAvailableFieldIterator(); iter.hasNext(); ) {
					String field = iter.next();
					chartRecord.setObjectValue(field, chart.getObjectValue(field));
				}
				chartRecord = Application.getCommonService().create(chartRecord);
				item.put("chart", chartRecord.getPrimary());
			}
			dashRecord.setString("config", dashCopy.toJSONString());
		}
		
		dashRecord = Application.getBean(DashboardConfigService.class).create(dashRecord);
		
		JSON ret = JSONUtils.toJSONObject("id", dashRecord.getPrimary());
		writeSuccess(response, ret);
	}
	
	@RequestMapping("/dash-config")
	public void dashConfig(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID dashid = getIdParameterNotNull(request, "id");
		JSON config = ServletUtils.getRequestJson(request);
		
		Record record = EntityHelper.forUpdate(dashid, getRequestUser(request));
		record.setString("config", config.toJSONString());
		Application.getBean(DashboardConfigService.class).update(record);
		writeSuccess(response);
	}
	
	@RequestMapping("/chart-list")
	public void chartList(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		String type = request.getParameter("type");

		Object[][] charts = null;
		if ("builtin".equalsIgnoreCase(type)) {
			charts = new Object[0][];
		} else {
		    ID useBizz = user;
		    String sql = "select chartId,title,chartType,modifiedOn,belongEntity from ChartConfig where (1=1) and createdBy = ? order by modifiedOn desc";
			if (UserHelper.isAdmin(user)) {
                sql = sql.replace("createdBy = ", "createdBy.roleId = ");
                useBizz = RoleService.ADMIN_ROLE;
			}

			if ("entity".equalsIgnoreCase(type)) {
			    String entity = request.getParameter("entity");
			    Entity entityMeta = MetadataHelper.getEntity(entity);
			    String entitySql = String.format("belongEntity = '%s'", StringEscapeUtils.escapeSql(entity));
			    if (entityMeta.getMasterEntity() != null) {
                    entitySql += String.format(" or belongEntity = '%s'", entityMeta.getMasterEntity().getName());
                } else if (entityMeta.getSlaveEntity() != null) {
                    entitySql += String.format(" or belongEntity = '%s'", entityMeta.getSlaveEntity().getName());
                }

			    sql = sql.replace("1=1", entitySql);
            }

            charts = Application.createQueryNoFilter(sql).setParameter(1, useBizz).array();
			for (Object[] o : charts) {
			    o[3] = Moment.moment((Date) o[3]).fromNow();
			    o[4] = EasyMeta.getLabel(MetadataHelper.getEntity((String) o[4]));
            }
		}

		// 内置图表
        if (!"entity".equalsIgnoreCase(type)) {
            for (BuiltinChart b : ChartsFactory.getBuiltinCharts()) {
                Object[] c = new Object[]{ b.getChartId(), b.getChartTitle(), b.getChartType(), "内置" };
                charts = (Object[][]) ArrayUtils.add(charts, c);
            }
        }

		writeSuccess(response, charts);
	}
}
