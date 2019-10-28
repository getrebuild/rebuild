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

package com.rebuild.server.service.query;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Filter;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.query.NativeQuery;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.privileges.EntityQueryFilter;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

/**
 * 查询服务
 * 
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public class QueryFactory {
	
	private static final int QUERY_TIMEOUT = 10 * 1000;
	private static final int SLOW_LOGGER_TIME = 1000;

	final private PersistManagerFactory aPMFactory;

	protected QueryFactory(PersistManagerFactory aPMFactory) {
		super();
		this.aPMFactory = aPMFactory;
	}

	/**
	 * @param ajql
	 * @return
	 */
	public Query createQuery(String ajql) {
		return createQuery(ajql, Application.getCurrentUser());
	}
	
	/**
	 * @param ajql
	 * @param user
	 * @return
	 */
	public Query createQuery(String ajql, ID user) {
		return createQuery(ajql, Application.getSecurityManager().createQueryFilter(user));
	}
	
	/**
	 * @param ajql
	 * @return
	 */
	public Query createQueryNoFilter(String ajql) {
		return createQuery(ajql, EntityQueryFilter.ALLOWED);
	}
	
	/**
	 * @param ajql
	 * @param filter
	 * @return
	 */
	public Query createQuery(String ajql, Filter filter) {
		Assert.notNull(filter, "'filter' not be null");
		Query query = aPMFactory.createQuery(ajql)
				.setTimeout(QUERY_TIMEOUT)
				.setSlowLoggerTime(SLOW_LOGGER_TIME)
				.setFilter(filter);
		return query;
	}
	
	/**
	 * @param sql
	 * @return
	 */
	public NativeQuery createNativeQuery(String sql) {
		return aPMFactory.createNativeQuery(sql)
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

	/**
	 * @param recordId
	 * @param fields
	 * @return
	 */
	public Object[] unique(ID recordId, String ...fields) {
		String sql = buildUniqueSql(recordId, fields);
		return createQuery(sql).setParameter(1, recordId).unique();
	}

	/**
	 * @param recordId
	 * @param fields
	 * @return
	 */
	public Object[] uniqueNoFilter(ID recordId, String ...fields) {
		String sql = buildUniqueSql(recordId, fields);
		return createQueryNoFilter(sql).setParameter(1, recordId).unique();
	}

	/**
	 * @param recordId
	 * @param fields
	 * @return
	 */
	private String buildUniqueSql(ID recordId, String ...fields) {
		Assert.notNull(recordId, "[recordId] not be null");

		Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
		if (fields.length == 0) {
			fields = new String[] { entity.getPrimaryField().getName() };
		}

		StringBuilder sql = new StringBuilder("select ");
		sql.append(StringUtils.join(fields, ","))
				.append(" from ").append(entity.getName())
				.append(" where ")
				.append(entity.getPrimaryField().getName())
				.append(" = ?");
		return sql.toString();
	}
}
