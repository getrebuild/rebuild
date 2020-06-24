/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
import com.rebuild.server.service.bizz.privileges.RoleBaseQueryFilter;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

/**
 * 查询服务
 * 
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 * @see RoleBaseQueryFilter
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
		return createQuery(ajql, RoleBaseQueryFilter.ALLOWED);
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
