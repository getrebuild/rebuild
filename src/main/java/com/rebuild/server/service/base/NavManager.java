/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.rebuild.server.service.base;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
public class NavManager extends LayoutManager {

	/**
	 * @param user
	 * @return
	 */
	public static JSON getNav(ID user) {
		Object[] sets = getLayoutConfigRaw("", TYPE_NAVI);
		if (sets == null) {
			return null;
		}
		
		JSONObject json = new JSONObject();
		json.put("id", sets[0]);
		json.put("config", sets[1]);
		return json;
	}
	
	/**
	 * @return
	 */
	public static JSONArray getNavForPortal() {
		Object[] sets = getLayoutConfigRaw("", TYPE_NAVI);
		return sets == null ? JSON.parseArray("[]") : (JSONArray) sets[1];
	}
}
