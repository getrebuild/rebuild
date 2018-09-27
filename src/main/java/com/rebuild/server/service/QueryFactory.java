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

package com.rebuild.server.service;

import com.rebuild.server.Application;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.query.NativeQuery;

/**
 * 查询服务
 * 
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public class QueryFactory {
	
	public static final int QUERY_TIMEOUT = 5 * 1000;
	public static final int SLOW_LOGGER_TIME = 1 * 1000;

	final private PersistManagerFactory PM_FACTORY;

	protected QueryFactory(PersistManagerFactory persistManagerFactory) {
		super();
		this.PM_FACTORY = persistManagerFactory;
	}

	/**
	 * @param ajql
	 * @return
	 */
	public Query createQuery(String ajql) {
		return createQuery(ajql, Application.getCurrentCallerUser());
	}
	
	/**
	 * @param ajql
	 * @param user
	 * @return
	 */
	public Query createQuery(String ajql, ID user) {
		Query query = PM_FACTORY.createQuery(ajql)
				.setTimeout(QUERY_TIMEOUT)
				.setSlowLoggerTime(SLOW_LOGGER_TIME)
				.setFilter(Application.getSecurityManager().createQueryFilter(user));
		return query;
	}
	
	/**
	 * @param ajql
	 * @return
	 */
	public Query createQueryUnfiltered(String ajql) {
		Query query = PM_FACTORY.createQuery(ajql)
				.setTimeout(QUERY_TIMEOUT)
				.setSlowLoggerTime(SLOW_LOGGER_TIME);
		return query;
	}

	/**
	 * @param sql
	 * @return
	 */
	public NativeQuery createNativeQuery(String sql) {
		return PM_FACTORY.createNativeQuery(sql)
				.setTimeout(QUERY_TIMEOUT)
				.setSlowLoggerTime(SLOW_LOGGER_TIME);
	}

	/**
	 * @param ajql
	 * @return
	 */
	public Object[][] array(String ajql) {
		return createQuery(ajql).array();
	}

	/**
	 * @param ajql
	 * @return
	 */
	public Object[] unique(String ajql) {
		return createQuery(ajql).unique();
	}

	/**
	 * @param ajql
	 * @return
	 */
	public Record record(String ajql) {
		return createQuery(ajql).record();
	}
}
