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

package com.rebuild.server.helper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.net.URL;

/**
 * 黑名单词 src/main/resources/blacklist.json
 * More details https://github.com/fighting41love/funNLP
 * 
 * @author devezhao
 * @since 01/31/2019
 */
public class BlackList {
	
	private static JSONArray BLACKLIST = null;
	
	/**
	 * @param text
	 * @return
	 */
	public static boolean isBlack(String text) {
		loadBlackListIfNeed();
		return BLACKLIST.contains(text.toLowerCase());
	}

	/**
	 * @param text
	 * @return
	 */
	public static boolean isSQLKeyword(String text) {
		return ArrayUtils.contains(SQL_KWS, text.toUpperCase());
	}
	
	/**
	 * 加载黑名单列表
	 */
	synchronized
	private static void loadBlackListIfNeed() {
		if (BLACKLIST != null) {
			return;
		}
		
		URL url = BlackList.class.getClassLoader().getResource("blacklist.json");
		try {
			String s = IOUtils.toString(url, "UTF-8");
			BLACKLIST = JSON.parseArray(s);
		} catch (IOException e) {
			Application.LOG.error("Couldn't load [blacklist.json] file! This feature is missed : " + e);
			BLACKLIST = JSONUtils.EMPTY_ARRAY;
		}
	}

	// SQL 关键字
	private static final String[] SQL_KWS = new String[] {
			"SELECT", "DISTINCT",  "MAX", "MIN", "AVG", "SUM", "COUNT", "FROM",
			"WHERE", "AND", "OR", "ORDER", "BY", "ASC", "DESC", "GROUP", "HAVING",
			"WITH", "ROLLUP", "IS", "NOT", "NULL", "IN", "LIKE", "EXISTS", "BETWEEN", "TRUE", "FALSE"
	};
}
