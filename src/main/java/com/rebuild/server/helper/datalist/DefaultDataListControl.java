/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
public class DefaultDataListControl implements DataListControl {

	private Entity entity;
	private QueryParser queryParser;
	private ID user;
	
	/**
	 * @param query
	 * @param user
	 */
	public DefaultDataListControl(JSONObject query, ID user) {
		this.entity = MetadataHelper.getEntity(query.getString("entity"));
		this.queryParser = new QueryParser(query, this);
		this.user = user;
	}
	
	@Override
	public Entity getEntity() {
		return entity;
	}

	/**
	 * @return
	 */
	public QueryParser getQueryParser() {
		return queryParser;
	}

	@Override
	public String getDefaultFilter() {
	    // 隐藏系统用户
		if (queryParser.getEntity().getEntityCode() == EntityHelper.User) {
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

		DataListWrapper wrapper = new DataListWrapper(
				totalRows, array, query.getSelectItems(), query.getRootEntity());
		wrapper.setPrivilegesFilter(user, queryParser.getQueryJoinFields());
		return wrapper.toJson();
	}
}
