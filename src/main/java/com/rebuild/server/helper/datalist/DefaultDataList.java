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

package com.rebuild.server.helper.datalist;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;

/**
 * 数据列表控制器
 * 
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-20
 */
public class DefaultDataList implements DataList {

	private Entity entity;
	private QueryParser queryParser;
	private ID user;
	
	/**
	 * @param query
	 * @param user
	 */
	public DefaultDataList(JSONObject query, ID user) {
		this.entity = MetadataHelper.getEntity(query.getString("entity"));
		this.queryParser = new QueryParser(query, this);
		this.user = user;
	}
	
	@Override
	public Entity getEntity() {
		return entity;
	}

	@Override
	public String getDefaultFilter() {

		// 列表默认过滤
		int ec = queryParser.getEntity().getEntityCode();
		
		if (ec == EntityHelper.User) {
			return String.format("userId <> '%s'", UserService.SYSTEM_USER);
		}
		return null;
	}
	
	@Override
	public JSON getJSONResult() {
		int totalRows = 0;
		if (queryParser.isNeedReload()) {
			Object[] count = Application.getQueryFactory().createQuery(queryParser.toCountSql(), user).unique();
			totalRows = ObjectUtils.toInt(count[0]);
		}
		
		Query query = Application.getQueryFactory().createQuery(queryParser.toSql(), user);
		int[] limits = queryParser.getSqlLimit();
		Object[][] array = query.setLimit(limits[0], limits[1]).array();
		
		DataWrapper wrapper = new DataWrapper(
				totalRows, array, query.getSelectItems(), query.getRootEntity());
		wrapper.setPrivilegesFilter(user, queryParser.getQueryJoinFields());
		return wrapper.toJson();
	}
}
