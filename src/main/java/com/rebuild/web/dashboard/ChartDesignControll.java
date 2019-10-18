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
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.charts.ChartData;
import com.rebuild.server.business.charts.ChartsException;
import com.rebuild.server.business.charts.ChartsFactory;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.configuration.ChartConfigService;
import com.rebuild.server.service.configuration.DashboardConfigService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseEntityControll;
import com.rebuild.web.IllegalParameterException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author devezhao
 * @since 12/09/2018
 */
@Controller
@RequestMapping("/dashboard")
public class ChartDesignControll extends BaseEntityControll {

	@RequestMapping("/chart-design")
	public ModelAndView pageDesign(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ModelAndView mv = createModelAndView("/dashboard/chart-design.jsp");
		
		ID user = getRequestUser(request);
		String entity = getParameter(request, "source");
		ID chartId = getIdParameter(request, "id");
		
		Entity entityMeta;
		if (chartId != null) {
			Object[] chart = Application.createQueryNoFilter(
					"select belongEntity,title,config,createdBy from ChartConfig where chartId = ?")
					.setParameter(1, chartId)
					.unique();
			if (chart == null) {
				response.sendError(404, "无效图表: " + chartId);
				return null;
			}
			if (!UserHelper.isAdmin(user) && !user.equals(chart[3])) {
				response.sendError(403, "你不能修改他人的图表");
				return null;
			}
			
			mv.getModel().put("chartId", chartId);
			mv.getModel().put("chartTitle", chart[1]);
			mv.getModel().put("chartConfig", chart[2]);
			mv.getModel().put("chartOwningAdmin", UserHelper.isAdmin((ID) chart[3]));
			entityMeta = MetadataHelper.getEntity((String) chart[0]);
		} else if (entity != null) {
			mv.getModel().put("chartConfig", JSONUtils.EMPTY_OBJECT_STR);
			mv.getModel().put("chartOwningAdmin", UserHelper.isAdmin(user));
			entityMeta = MetadataHelper.getEntity(entity);
		} else {
			throw new IllegalParameterException("无效图表参数");
		}
		
		if (!Application.getSecurityManager().allowedR(getRequestUser(request), entityMeta.getEntityCode())) {
			response.sendError(403, "你没有读取 [" + EasyMeta.getLabel(entityMeta) + "] 的权限，因此无法设计此图表");
			return null;
		}
		
		putEntityMeta(mv, entityMeta);

		List<String[]> fields = new ArrayList<>();
		for (Field field : MetadataSorter.sortFields(entityMeta)) {
			EasyMeta easy = EasyMeta.valueOf(field);
			DisplayType dt = easy.getDisplayType();
			if (dt == DisplayType.IMAGE || dt == DisplayType.FILE || dt == DisplayType.ANYREFERENCE) {
				continue;
			}
			String type = "text";
			if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
				type = "date";
			} else if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
				type = "num";
			}
			fields.add(new String[] { easy.getName(), easy.getLabel(), type });
		}
		mv.getModel().put("fields", fields);
		
		return mv;
	}
	
	@RequestMapping("/chart-preview")
	public void dataPreview(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSON config = ServletUtils.getRequestJson(request);
		JSON data;
		try {
			ChartData chart = ChartsFactory.create((JSONObject) config, getRequestUser(request));
			data = chart.build(true);
		} catch (ChartsException ex) {
			writeFailure(response, ex.getLocalizedMessage());
			return;
		}
		writeSuccess(response, data);
	}
	
	@RequestMapping("/chart-save")
	public void chartSave(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);

		Record record = EntityHelper.parse((JSONObject) formJson, user);
		ID dashid = null;
		if (record.getPrimary() == null) {
			dashid = getIdParameterNotNull(request, "dashid");
		}
		record = Application.getBean(ChartConfigService.class).createOrUpdate(record);
		
		// 添加到仪表盘
		if (dashid != null) {
			Object[] dash = Application.createQueryNoFilter(
					"select config from DashboardConfig where configId = ?")
					.setParameter(1, dashid)
					.unique();
			JSONArray config = JSON.parseArray((String) dash[0]);
			
			JSONObject item = JSONUtils.toJSONObject("chart", record.getPrimary());
			item.put("w", 4);
			item.put("h", 4);
			config.add(item);
			
			Record dashRecord = EntityHelper.forUpdate(dashid, getRequestUser(request));
			dashRecord.setString("config", config.toJSONString());
			Application.getBean(DashboardConfigService.class).createOrUpdate(dashRecord);
		}
		
		JSONObject ret = JSONUtils.toJSONObject("id", record.getPrimary());
		writeSuccess(response, ret);
	}

    @RequestMapping("/chart-delete")
    public void chartDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
	    // TODO 不能删除他人图表
        ID chartId = getIdParameterNotNull(request, "id");
        Application.getBean(ChartConfigService.class).delete(chartId);
        writeSuccess(response);
    }
}
