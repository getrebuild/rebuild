/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.configuration.portals;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.configuration.DashboardConfigService;
import com.rebuild.utils.JSONUtils;

import java.util.Arrays;
import java.util.Comparator;

/**
 * 首页仪表盘
 * 
 * @author devezhao
 * @since 12/20/2018
 */
public class DashboardManager extends ShareToManager {
	
	public static final DashboardManager instance = new DashboardManager();
	private DashboardManager() { }

	@Override
	protected String getConfigEntity() {
		return "DashboardConfig";
	}

	@Override
	protected String getConfigFields() {
		return super.getConfigFields() + ",title";
	}

	/**
	 * 获取可用面板
	 * 
	 * @param user
	 * @return
	 */
	public JSON getDashList(ID user) {
		ID detected = detectUseConfig(user, null, null);
		// 没有就初始化一个
		if (detected == null) {
			Record record = EntityHelper.forNew(EntityHelper.DashboardConfig, user);
			record.setString("config", JSONUtils.EMPTY_ARRAY_STR);
			record.setString("title", UserHelper.isAdmin(user) ? "默认仪表盘" : "我的仪表盘");
			record.setString("shareTo", UserHelper.isAdmin(user) ? SHARE_ALL : SHARE_SELF);
			Application.getBean(DashboardConfigService.class).create(record);
		}

		Object[][] canUses = getUsesConfig(user, null, null);
		// 补充图表标题
		for (int i = 0; i < canUses.length; i++) {
			JSONArray charts = JSON.parseArray((String) canUses[i][3]);
			ChartManager.instance.richingCharts(charts);
			canUses[i][3] = charts;
			canUses[i][2] = isSelf(user, (ID) canUses[i][2]);
		}

		Arrays.sort(canUses, Comparator.comparing(o -> o[4].toString()));
		return (JSON) JSON.toJSON(canUses);
	}

	@Override
	public void clean(Object cacheKey) {
		Application.getCommonCache().evict(formatCacheKey(null, null));
	}
}
