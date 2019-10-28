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

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 高级过滤器
 * 
 * @author devezhao
 * @since 09/30/2018
 */
public class AdvFilterManager extends ShareToManager<ID> {
	
	public static final AdvFilterManager instance = new AdvFilterManager();
	private AdvFilterManager() { }

	@Override
	protected String getConfigEntity() {
		return "FilterConfig";
	}

	@Override
	protected String getFieldsForConfig() {
		return super.getFieldsForConfig() + ",filterName";
	}

	/**
	 * 获取高级查询列表
	 *
	 * @param entity
	 * @param user
	 * @return
	 */
	public JSONArray getAdvFilterList(String entity, ID user) {
		Object[][] canUses = getUsesConfig(user, entity, null);

		List<ConfigEntry> ces = new ArrayList<>();
		for (Object[] c : canUses) {
			ConfigEntry e = new ConfigEntry()
					.set("id", c[0])
					.set("editable", isSelf(user, (ID) c[2]))
					.set("name", c[4]);
			ces.add(e);
		}
		return JSONUtils.toJSONArray(ces.toArray(new ConfigEntry[0]));
	}

	/**
	 * 获取高级查询
	 * 
	 * @param cfgid
	 * @return
	 */
	public ConfigEntry getAdvFilter(ID cfgid) {
		Object[] o = Application.createQueryNoFilter(
				"select belongEntity from FilterConfig where configId = ?")
				.setParameter(1, cfgid)
				.unique();
		if (o == null) {
			throw new RebuildException("No config found : " + cfgid);
		}

		Object[][] cached = getAllConfig((String) o[0], null);
		for (Object[] c : cached) {
			if (!c[0].equals(cfgid)) continue;
			return new ConfigEntry()
					.set("id", c[0])
					.set("shareTo", c[1])
					.set("name", c[4])
					.set("filter", JSON.parse((String) c[3]));
		}
		return null;
	}
	
	@Override
	public void clean(ID cacheKey) {
		cleanWithBelongEntity(cacheKey, false);
	}
}