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

package com.rebuild.web.base.entity.datalist;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;

import cn.devezhao.persist4j.Query;

/**
 * 数据列表控制器
 * 
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-20
 */
public class DefaultDataListControl implements DataListControl {

	protected static final int READ_TIMEOUT = 15 * 1000;
	
	protected JsonQueryParser queryParser;

	/**
	 */
	protected DefaultDataListControl() {
	}
	
	/**
	 * @param queryElement
	 */
	public DefaultDataListControl(JSONObject queryElement) {
		this.queryParser = new JsonQueryParser(queryElement, this);
	}

	/**
	 * @return
	 */
	public JsonQueryParser getQueryParser() {
		return queryParser;
	}

	@Override
	public String getDefaultFilter() {
		return null;
	}
	
	@Override
	public String getResult() {
		int total = 0;
		if (queryParser.isNeedReload()) {
			String countSql = queryParser.toSqlCount();
			total = ((Long) Application.createQuery(countSql).unique()[0]).intValue();
		}
		
		Query query = Application.createQuery(queryParser.toSql()).setTimeout(READ_TIMEOUT);
		int[] limits = queryParser.getSqlLimit();
		Object[][] array = query.setLimit(limits[0], limits[1]).array();
		
		DataWrapper wrapper = new DataWrapper(
				total, array, query.getSelectItems(), query.getRootEntity());
		return wrapper.toJson();
	}
}
