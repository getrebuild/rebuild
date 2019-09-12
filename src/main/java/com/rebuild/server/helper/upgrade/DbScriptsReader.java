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

package com.rebuild.server.helper.upgrade;

import cn.devezhao.commons.ObjectUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parseing `scripts/mysql-upgrade.sql`
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/22
 */
public class DbScriptsReader {
	
	private static final String TAG_STARTS = "-- #";
	private static final String TAG_COMMENT = "--";
	
	/**
	 * @return
	 * @throws IOException
	 */
	public Map<Integer, String[]> read() throws IOException {
		InputStream is = DbScriptsReader.class.getClassLoader().getResourceAsStream("scripts/db-upgrade.sql");
		List<String> sqlScripts = null;
		try {
			sqlScripts = IOUtils.readLines(is, "utf-8");
		} finally {
			IOUtils.closeQuietly(is);
		}
		
		Map<Integer, String[]> sqls = new HashMap<>();
		
		int oneVer = -1;
		List<String> sqlBatch = new ArrayList<>();
		StringBuffer sqlOne = new StringBuffer();
		
		for (String sl : sqlScripts) {
			if (StringUtils.isBlank(sl)) {
				continue;
			}
			
			if (sl.startsWith(TAG_STARTS)) {
				if (oneVer > -1) {
					sqls.put(oneVer, sqlBatch.toArray(new String[0]));
				}
				
				// reset
				String ver = sl.substring(TAG_STARTS.length()).split(" ")[0];  // eg: -- #2 abc
				oneVer = ObjectUtils.toInt(ver);
				sqlBatch = new ArrayList<>();
				
			} else if (sl.startsWith(TAG_COMMENT)) {
				// Ignore comments
			} else {
				sqlOne.append(sl).append("\n");
				if (sl.endsWith(";")) {  // SQL end by `;`
					sqlBatch.add(sqlOne.toString());
					sqlOne = new StringBuffer();
				}
			}
		}
		
		if (sqlOne.length() > 0) {
			sqlBatch.add(sqlOne.toString());
		}
		if (oneVer > -1) {
			sqls.put(oneVer, sqlBatch.toArray(new String[0]));
		}
		
		return sqls;
	}
}
