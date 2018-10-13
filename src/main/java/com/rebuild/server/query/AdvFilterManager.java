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

package com.rebuild.server.query;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.entityhub.EasyMeta;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.persist4j.Entity;

/**
 * 
 * @author devezhao
 * @since 09/30/2018
 */
public class AdvFilterManager {
	
	private static final Log LOG = LogFactory.getLog(AdvFilterManager.class);
	
	/**
	 * 基本查询过滤条件特别名称
	 */
	public static final String FN_SIMPLE = "$SIMPLE$";

	/**
	 * @param entity
	 * @return
	 */
	public static Object[] getSimpleFilterRaw(String entity) {
		Object[] raw = Application.createNoFilterQuery(
				"select filterId,config from FilterConfig where filterName = ? and belongEntity = ?")
				.setParameter(1, FN_SIMPLE)
				.setParameter(2, entity)
				.unique();
		if (raw == null) {
			return null;
		}
		
		Entity metaEntity = MetadataHelper.getEntity(entity);
		JSONObject config = (JSONObject) JSON.parse((String) raw[1]);
		JSONArray items = config.getJSONArray("items");
		for (Iterator<Object> iter = items.iterator(); iter.hasNext(); ) {
			JSONObject item = (JSONObject) iter.next();
			String field = item.getString("field");
			if (!metaEntity.containsField(field)) {
				LOG.warn("Unknow field '" + field + "' in '" + entity + "'");
				continue;
			}
			
			String label = EasyMeta.getLabel(metaEntity.getField(field));
			item.put("label", label);
		}
		raw[1] = config;
		return raw;
	}
}