/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.persist4j.*;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.query.NativeQuery;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.RoleBaseQueryFilter;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * 查询服务
 *
 * @author zhaofang123@gmail.com
 * @see RoleBaseQueryFilter
 * @since 05/21/2017
 */
@Service
public class QueryFactory {

    private static final int QUERY_TIMEOUT = 10 * 1000;
    private static final int SLOW_LOGGER_TIME = 1000;

    private final PersistManagerFactory aPMFactory;

    protected QueryFactory(PersistManagerFactory aPMFactory) {
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
        return createQuery(ajql, Application.getPrivilegesManager().createQueryFilter(user));
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
        Assert.notNull(filter, "[filter] cannot be null");
        return aPMFactory.createQuery(ajql)
                .setTimeout(QUERY_TIMEOUT)
                .setSlowLoggerTime(SLOW_LOGGER_TIME)
                .setFilter(filter);
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
    public Object[] unique(ID recordId, String... fields) {
        String sql = buildUniqueSql(recordId, fields);
        return createQuery(sql).setParameter(1, recordId).unique();
    }

    /**
     * @param recordId
     * @param fields
     * @return
     */
    public Object[] uniqueNoFilter(ID recordId, String... fields) {
        String sql = buildUniqueSql(recordId, fields);
        return createQueryNoFilter(sql).setParameter(1, recordId).unique();
    }

    /**
     * @param recordId
     * @param fields
     * @return
     */
    private String buildUniqueSql(ID recordId, String... fields) {
        Assert.notNull(recordId, "[recordId] cannot be null");

        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
        if (fields.length == 0) {
            fields = new String[]{entity.getPrimaryField().getName()};
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
