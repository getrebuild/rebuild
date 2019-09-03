/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.configuration;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.JSONable;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/20
 */
public class ConfigEntry implements Serializable, Cloneable, JSONable {
	private static final long serialVersionUID = -2618040374508703332L;
	
	private Map<String, Object> entryMap = null;

	public ConfigEntry() {
		this.entryMap = new HashMap<>();
	}
	
	/**
	 * @param name
	 * @param value Remove if null
	 * @return
	 */
	public ConfigEntry set(String name, Object value) {
		Assert.notNull(name, "'name' must not be null");
		if (value == null) {
			entryMap.remove(name);
		} else {
			entryMap.put(name, value);
		}
		return this;
	}
	
	public ID getID(String name) {
		return (ID) entryMap.get(name);
	}
	
	public String getString(String name) {
		return (String) entryMap.get(name);
	}
	
	public Boolean getBoolean(String name) {
		return (Boolean) entryMap.get(name);
	}
	
	public Integer getInteger(String name) {
		return (Integer) entryMap.get(name);
	}
	
	public JSON getJSON(String name) {
		return (JSON) entryMap.get(name);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String name, Class<T> returnType) {
		return (T) entryMap.get(name);
	}
	
	@Override
	public ConfigEntry clone() {
		ConfigEntry c = new ConfigEntry();
		for (Map.Entry<String, Object> e : this.entryMap.entrySet()) {
			Object v = e.getValue();
			if (v instanceof JSON) {
				v = JSONUtils.clone((JSON) v);
			}
			c.set(e.getKey(), v);
		}
		return c;
	}
	
	@Override
	public JSON toJSON() {
		return (JSONObject) JSON.toJSON(this.entryMap);
	}
	
	@Override
	public JSON toJSON(String... special) {
		Map<String, Object> map = new HashMap<>();
		for (String s : special) {
			map.put(s, entryMap.get(s));
		}
		return (JSONObject) JSON.toJSON(map);
	}
}
