/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.configuration.portals;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import com.rebuild.server.configuration.ConfigEntry;

/**
 * 基础布局管理
 * 
 * @author zhaofang123@gmail.com
 * @since 09/15/2018
 */
public class BaseLayoutManager extends ShareToManager {
	
	public static final BaseLayoutManager instance = new BaseLayoutManager();
	protected BaseLayoutManager() { }

	// 导航
	public static final String TYPE_NAV = "NAV";
	// 表单
	public static final String TYPE_FORM = "FORM";
	// 列表
	public static final String TYPE_DATALIST = "DATALIST";
	// 视图-相关项
	public static final String TYPE_TAB = "TAB";
	// 视图-新建相关
	public static final String TYPE_ADD = "ADD";
	// 列表-图表 of Widget
	public static final String TYPE_WCHARTS = "WCHARTS";

	@Override
	protected String getConfigEntity() {
		return "LayoutConfig";
	}

	/**
	 * @param user
	 * @param entity
	 * @return
	 */
	public ConfigEntry getLayoutOfForm(ID user, String entity) {
		return getLayout(user, entity, TYPE_FORM);
	}

	/**
	 * @param user
	 * @param entity
	 * @return
	 */
	public ConfigEntry getLayoutOfDatalist(ID user, String entity) {
		return getLayout(user, entity, TYPE_DATALIST);
	}

	/**
	 * @param user
	 * @return
	 */
	public ConfigEntry getLayoutOfNav(ID user) {
		return getLayout(user, null, TYPE_NAV);
	}

	/**
	 * 列表页 SIDE 图表
	 *
	 * @param user
	 * @param entity
	 * @return
	 */
	public ConfigEntry getWidgetCharts(ID user, String entity) {
		ConfigEntry e = getLayout(user, entity, TYPE_WCHARTS);
		if (e == null) {
            return null;
        }

		// 补充图表信息
		JSONArray charts = (JSONArray) e.getJSON("config");
		ChartManager.instance.richingCharts(charts);
		return e.set("config", charts)
				.set("shareTo", null);
	}

	/**
	 * @param user
	 * @param belongEntity
	 * @param applyType
	 * @return
	 */
	public ConfigEntry getLayout(ID user, String belongEntity, String applyType) {
		ID detected = detectUseConfig(user, belongEntity, applyType);
		if (detected == null) {
			return null;
		}

		Object[][] cached = getAllConfig(belongEntity, applyType);
		return findEntry(cached, detected);
	}

	/**
	 * @param cfgid
	 * @return
	 */
	public ConfigEntry getLayoutById(ID cfgid) {
		Object[] o = Application.createQueryNoFilter(
				"select belongEntity,applyType from LayoutConfig where configId = ?")
				.setParameter(1, cfgid)
				.unique();
		if (o == null) {
			throw new RebuildException("No config found : " + cfgid);
		}

		Object[][] cached = getAllConfig((String) o[0], (String) o[1]);
		return findEntry(cached, cfgid);
	}

    /**
     * @param uses
     * @param cfgid
     * @return
     */
	protected ConfigEntry findEntry(Object[][] uses, ID cfgid) {
        for (Object[] c : uses) {
            if (c[0].equals(cfgid)) {
				return new ConfigEntry()
						.set("id", c[0])
						.set("shareTo", c[1])
						.set("config", JSON.parse((String) c[3]));
            }
        }
        return null;
    }

	@Override
	public void clean(Object layoutId) {
		cleanWithBelongEntity((ID) layoutId, true);
	}
}
