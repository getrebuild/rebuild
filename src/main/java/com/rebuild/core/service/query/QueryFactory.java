/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Filter;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.query.NativeQuery;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.RoleBaseQueryFilter;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 查询服务
 *
 * @author Zixin (RB)
 * @since 05/21/2019
 * @see RoleBaseQueryFilter
 * @see QueryHelper
 */
@Service
public class QueryFactory {

    private static final int QUERY_TIMEOUT = 15;  // s
    private static final int SLOW_LOGGER_TIME = 3 * 1000;  // ms

    private final PersistManagerFactory aPMFactory;

    protected QueryFactory(PersistManagerFactory aPMFactory) {
        this.aPMFactory = aPMFactory;
    }

    /**
     * @param ajql
     * @return
     * @see #createQueryNoFilter(String)
     */
    public Query createQuery(String ajql) {
        return createQuery(ajql, UserContextHolder.getUser());
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
     * @param filter
     * @return
     * @see PersistManagerFactory#createQuery(String, Filter)
     */
    public Query createQuery(String ajql, Filter filter) {
        Assert.notNull(filter, "[filter] cannot be null");
        return new QueryDecorator(ajql, aPMFactory, filter)
                .setTimeout(QUERY_TIMEOUT)
                .setSlowLoggerTime(SLOW_LOGGER_TIME)
                .setFilter(filter);
    }

    /**
     * 1.无权限实体查询（无权限实体使用 #createQuery 除管理员外将查不到数据）
     * 2.有权限实体查询单不应用角色（权限）
     *
     * @param ajql
     * @return
     * @see #createQuery(String)
     */
    public Query createQueryNoFilter(String ajql) {
        return createQuery(ajql, RoleBaseQueryFilter.ALLOWED);
    }

    /**
     * 原生 SQL 查询
     *
     * @param rawSql
     * @return
     */
    public NativeQuery createNativeQuery(String rawSql) {
        return aPMFactory.createNativeQuery(rawSql)
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
     * @param fields 不指定则返所有字段
     * @return
     */
    public Object[] unique(ID recordId, String... fields) {
        String sql = buildUniqueSql(recordId, fields);
        return createQuery(sql).setParameter(1, recordId).unique();
    }

    /**
     * @param recordId
     * @param fields 不指定则返所有字段
     * @return
     */
    public Object[] uniqueNoFilter(ID recordId, String... fields) {
        String sql = buildUniqueSql(recordId, fields);
        return createQueryNoFilter(sql).setParameter(1, recordId).unique();
    }

    /**
     * @param recordId
     * @param fields 不指定则返所有字段
     * @return
     */
    public Record record(ID recordId, String... fields) {
        String sql = buildUniqueSql(recordId, fields);
        return createQuery(sql).setParameter(1, recordId).record();
    }

    /**
     * @param recordId
     * @param fields 不指定则返所有字段
     * @return
     */
    public Record recordNoFilter(ID recordId, String... fields) {
        String sql = buildUniqueSql(recordId, fields);
        return createQueryNoFilter(sql).setParameter(1, recordId).record();
    }

    private String buildUniqueSql(ID recordId, String... fields) {
        Assert.notNull(recordId, "[recordId] cannot be null");

        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
        List<String> selectFields = new ArrayList<>();
        if (fields.length == 0) {
            for (Field field : entity.getFields()) selectFields.add(field.getName());
        } else {
            Collections.addAll(selectFields, fields);
        }

        String pkName = entity.getPrimaryField().getName();
        // fix: v3.6
        if (!selectFields.contains(pkName)) selectFields.add(pkName);

        return String.format("select %s from %s where %s = ?",
                StringUtils.join(selectFields, ","), entity.getName(), pkName);
    }
}
