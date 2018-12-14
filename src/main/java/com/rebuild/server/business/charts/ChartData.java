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

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.persist4j.Entity;

/**
 * 
 * @author devezhao
 * @since 12/14/2018
 */
public abstract class ChartData {
	
	protected JSONObject config;
	
	/**
	 * @param config
	 */
	protected ChartData(JSONObject config) {
		this.config = config;
	}
	
	/**
	 * 标题
	 * 
	 * @return
	 */
	public String getTitle() {
		return StringUtils.defaultIfBlank(config.getString("title"), "未命名图表");
	}
	
	/**
	 * 源实体
	 * 
	 * @return
	 */
	public Entity getSourceEntity() {
		String e = config.getString("entity");
		return MetadataHelper.getEntity(e);
	}
	
	/**
	 * 维度
	 * 
	 * @return
	 */
	public JSONArray getAxisDimension() {
		return config.getJSONArray("dimension");
	}
	
	/**
	 * 数值
	 * 
	 * @return
	 */
	public JSONArray getAxisNumerical() {
		return config.getJSONArray("numerical");
	}
	
	abstract public String toData();
	
}
