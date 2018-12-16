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

package com.rebuild.server.business.charts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;

import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 12/15/2018
 */
public class ChartDataBuilder {

	/**
	 * @param chartId
	 * @return
	 */
	public static ChartData newChartData(ID chartId) {
		Object[] chart = Application.createQuery(
				"select config from ChartConfig where chartId = ?")
				.setParameter(1, chartId)
				.unique();
		JSONObject config = JSON.parseObject((String) chart[0]);
		return newChartData(config);
	}
	
	/**
	 * @param chartConfig
	 * @return
	 */
	public static ChartData newChartData(JSONObject chartConfig) {
		String type = chartConfig.getString("type");
		if ("INDEX".equalsIgnoreCase(type)) {
			return new IndexChart(chartConfig);
		} else if ("TABLE".equalsIgnoreCase(type)) {
			return new TableChart(chartConfig);
		} else if ("LINE".equalsIgnoreCase(type)) {
			return new LineChart(chartConfig);
		} else if ("BAR".equalsIgnoreCase(type)) {
			return new BarChart(chartConfig);
		} else if ("PIE".equalsIgnoreCase(type)) {
			return new PieChart(chartConfig);
		} else if ("FUNNEL".equalsIgnoreCase(type)) {
			return new FunnelChart(chartConfig);
		}
		throw new UnsupportedOperationException(type);
	}
	
}
