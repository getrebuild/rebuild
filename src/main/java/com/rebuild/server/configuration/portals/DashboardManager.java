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

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.configuration.DashboardConfigService;
import com.rebuild.utils.JSONUtils;

/**
 * 首页仪表盘
 * 
 * @author devezhao
 * @since 12/20/2018
 */
public class DashboardManager extends ShareToManager<ID> {
	
	public static final DashboardManager instance = new DashboardManager();
	private DashboardManager() { }

	@Override
	protected String getConfigEntity() {
		return "DashboardConfig";
	}

	@Override
	protected String getFieldsForConfig() {
		return super.getFieldsForConfig() + ",title";
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

		sort(canUses, 4);
		return (JSON) JSON.toJSON(canUses);
	}

	@Override
	public void clean(ID cacheKey) {
		Application.getCommonCache().evict(formatCacheKey(null, null));
	}
}
