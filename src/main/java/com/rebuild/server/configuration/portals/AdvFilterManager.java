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
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 高级过滤器
 * 
 * @author devezhao
 * @since 09/30/2018
 */
public class AdvFilterManager extends ShareToManager {
	
	public static final AdvFilterManager instance = new AdvFilterManager();
	private AdvFilterManager() { }

	@Override
	protected String getConfigEntity() {
		return "FilterConfig";
	}

	@Override
	protected String getConfigFields() {
		return super.getConfigFields() + ",filterName";
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
			if (c[0].equals(cfgid)) {
                return new ConfigEntry()
                        .set("id", c[0])
                        .set("shareTo", c[1])
                        .set("name", c[4])
                        .set("filter", JSON.parse((String) c[3]));
            }
		}
		return null;
	}
	
	@Override
	public void clean(Object filterId) {
		cleanWithBelongEntity((ID) filterId, false);
	}
}