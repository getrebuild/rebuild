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

package com.rebuild.server.business.charts;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.charts.builtin.ApprovalList;
import com.rebuild.server.business.charts.builtin.BuiltinChart;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.portals.ChartManager;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;

/**
 * @author devezhao
 * @since 12/15/2018
 */
public class ChartsFactory {

	/**
	 * @param chartId
	 * @return
	 * @throws ChartsException
	 */
	public static ChartData create(ID chartId) throws ChartsException {
		ConfigEntry chart = ChartManager.instance.getChart(chartId);
		if (chart == null) {
			throw new ChartsException("无效图表");
		}
		
		JSONObject config = (JSONObject) chart.getJSON("config");
		config.put("chartOwning", chart.getID("createdBy"));
		return create(config, Application.getCurrentUser());
	}
	
	/**
	 * @param config
	 * @param user
	 * @return
	 * @throws ChartsException
	 */
	public static ChartData create(JSONObject config, ID user) throws ChartsException {
		String e = config.getString("entity");
		if (!MetadataHelper.containsEntity(e)) {
			throw new ChartsException("源实体 [" + e + "] 不存在");
		}

		Entity entity = MetadataHelper.getEntity(e);
		if (user == null || !Application.getSecurityManager().allowedR(user, entity.getEntityCode())) {
			throw new ChartsException("没有读取 [" + EasyMeta.getLabel(entity) + "] 的权限");
		}

		String type = config.getString("type");
		if ("INDEX".equalsIgnoreCase(type)) {
			return new IndexChart(config).setUser(user);
		} else if ("TABLE".equalsIgnoreCase(type)) {
			return new TableChart(config).setUser(user);
		} else if ("LINE".equalsIgnoreCase(type)) {
			return new LineChart(config).setUser(user);
		} else if ("BAR".equalsIgnoreCase(type)) {
			return new BarChart(config).setUser(user);
		} else if ("PIE".equalsIgnoreCase(type)) {
			return new PieChart(config).setUser(user);
		} else if ("FUNNEL".equalsIgnoreCase(type)) {
			return new FunnelChart(config).setUser(user);
		} else if ("TREEMAP".equalsIgnoreCase(type)) {
			return new TreemapChart(config).setUser(user);
		} else {
			for (BuiltinChart ch : getBuiltinCharts()) {
				if (ch.getChartType().equalsIgnoreCase(type)) {
					return ((ChartData) ch).setUser(user);
				}
			}
		}
		throw new ChartsException("未知的图表类型 : " + type);
	}

	/**
	 * 获取内建图表
	 *
	 * @return
	 */
	public static BuiltinChart[] getBuiltinCharts() {
		return new BuiltinChart[] {
				new ApprovalList()
		};
	}
}
