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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;

/**
 * 数据列表控制器
 * 
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-20
 */
public class DefaultDataListControl implements DataListControl {

	protected static final int READ_TIMEOUT = 15 * 1000;
	
	private JSONQueryParser queryParser;
	private ID user;

	protected DefaultDataListControl() {
	}
	
	/**
	 * @param query
	 * @param user
	 */
	public DefaultDataListControl(JSONObject query, ID user) {
		this.queryParser = new JSONQueryParser(query, this);
		this.user = user;
	}

	/**
	 * @return
	 */
	public JSONQueryParser getQueryParser() {
		return queryParser;
	}

	@Override
	public String getDefaultFilter() {
		return null;
	}
	
	@Override
	public JSON getResult() {
		int totalRows = 0;
		if (queryParser.isNeedReload()) {
			Object[] count = Application.getQueryFactory().createQuery(queryParser.toSqlCount(), user).unique();
			totalRows = ObjectUtils.toInt(count[0]);
		}
		
		Query query = Application.getQueryFactory().createQuery(queryParser.toSql(), user);
		int[] limits = queryParser.getSqlLimit();
		Object[][] array = query.setLimit(limits[0], limits[1]).array();
		
		DataWrapper wrapper = new DataWrapper(
				totalRows, array, query.getSelectItems(), query.getRootEntity());
		return wrapper.toJson();
	}
}
