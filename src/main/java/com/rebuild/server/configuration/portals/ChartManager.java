/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.configuration.portals;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.charts.ChartsFactory;
import com.rebuild.server.business.charts.builtin.BuiltinChart;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.ConfigManager;
import com.rebuild.server.service.bizz.UserService;

import java.util.Iterator;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/06/04
 */
public class ChartManager implements ConfigManager {

	public static final ChartManager instance = new ChartManager();
	private ChartManager() { }
	
	/**
	 * @param chartid
	 * @return
	 */
	public ConfigEntry getChart(ID chartid) {
		final String ckey = "Chart-" + chartid;
		ConfigEntry entry = (ConfigEntry) Application.getCommonCache().getx(ckey);
		if (entry != null) {
			return entry.clone();
		}

		Object[] o = Application.createQueryNoFilter(
				"select title,chartType,config,createdBy from ChartConfig where chartId = ?")
				.setParameter(1, chartid)
				.unique();
		if (o == null) {
			for (BuiltinChart ch : ChartsFactory.getBuiltinCharts()) {
				if (chartid.equals(ch.getChartId())) {
					o = new Object[] { ch.getChartTitle(), ch.getChartType(), ch.getChartConfig(), UserService.SYSTEM_USER };
				}
			}

			if (o == null) {
				return null;
			}
		}
		
		entry = new ConfigEntry()
				.set("title", o[0])
				.set("type", o[1])
				.set("config", o[2] instanceof JSON ? (JSON) o[2] : JSON.parse((String) o[2]))
				.set("createdBy", o[3]);
		Application.getCommonCache().putx(ckey, entry);
		return entry.clone();
	}

	/**
	 * 丰富图表数据 title, type
	 *
	 * @param charts
	 */
	protected void richingCharts(JSONArray charts) {
        for (Iterator<Object> iter = charts.iterator(); iter.hasNext(); ) {
            JSONObject ch = (JSONObject) iter.next();
            ID chartid = ID.valueOf(ch.getString("chart"));
            ConfigEntry e = getChart(chartid);
            if (e == null) {
                iter.remove();
                continue;
            }

            ch.put("title", e.getString("title"));
            ch.put("type", e.getString("type"));
        }
	}

	@Override
	public void clean(Object chartId) {
		Application.getCommonCache().evict("Chart-" + chartId);
	}
}
