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

package com.rebuild.server.helper;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.utils.JSONUtils;

/**
 * 黑名单
 * 
 * @author devezhao
 * @since 01/31/2019
 */
public class BlackList {
	
	private static JSONArray BLACKLIST = null;
	
	/**
	 * @param name
	 * @return
	 */
	public static boolean isBlack(String name) {
		loadBlackListIfNeed();
		return BLACKLIST.contains(name.toLowerCase());
	}
	
	/**
	 * 加载黑名单列表
	 */
	private static void loadBlackListIfNeed() {
		if (BLACKLIST != null) {
			return;
		}
		
		URL url = BlackList.class.getClassLoader().getResource("blacklist.json");
		try {
			String s = IOUtils.toString(url, "UTF-8");
			BLACKLIST = JSON.parseArray(s);
		} catch (IOException e) {
			Application.LOG.error("Cloud't load blacklist! The feature is missed : " + e);
			BLACKLIST = JSONUtils.EMPTY_ARRAY;
		}
	}
}
