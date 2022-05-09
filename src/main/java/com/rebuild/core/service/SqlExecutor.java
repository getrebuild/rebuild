/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import cn.devezhao.persist4j.DataAccessException;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.JdbcSupport;
import cn.devezhao.persist4j.engine.StatementCallback;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 原生 SQL 执行者
 *
 * @author <a href="mailto:zhaofang123@gmail.com">Zhao Fangfang</a>
 * @since 0.1, Mar 31, 2010
 */
@Service
public class SqlExecutor {

    private static final int MAX_BATCH_SIZE = 100;

    final private PersistManagerFactory aPMFactory;

    protected SqlExecutor(PersistManagerFactory factory) {
        this.aPMFactory = factory;
    }

    /**
     * @param sql
     * @return
     */
    public int execute(String sql) {
        return execute(sql, 60);
    }

    /**
     * @param sql
     * @param timeout in seconds
     * @return
     */
    public int execute(String sql, int timeout) {
        try {
            final JdbcSupport jdbcSupport = (JdbcSupport) aPMFactory.createPersistManager();
            jdbcSupport.setTimeout(timeout);

            return jdbcSupport.execute(new StatementCallback() {
                @Override
                public Object doInParameters(PreparedStatement pstmt) {
                    return null;
                }

                @Override
                public String getSql() {
                    return sql;
                }
            });
        } catch (Exception ex) {
            throw new DataAccessException("SQL#: " + sql, ex);
        }
    }

    /**
     * @param sqls
     * @return
     */
    public int executeBatch(String[] sqls) {
        return executeBatch(sqls, 60 * 3);
    }

    /**
     * @param sqls
     * @param timeout
     * @return
     */
    public int executeBatch(String[] sqls, int timeout) {
        int affected = 0;
        List<String> tmp = new ArrayList<>();
        for (String sql : sqls) {
            tmp.add(sql);
            if (tmp.size() == MAX_BATCH_SIZE) {
                affected += this.executeBatchInternal(tmp, timeout);
                tmp.clear();
            }
        }

        if (!tmp.isEmpty()) {
            affected += this.executeBatchInternal(tmp, timeout);
        }
        return affected;
    }

    /**
     * @param sqls
     * @param timeout
     * @return
     */
    private int executeBatchInternal(Collection<String> sqls, int timeout) {
        int affected = 0;
        try {
            final JdbcSupport jdbcSupport = (JdbcSupport) aPMFactory.createPersistManager();
            jdbcSupport.setTimeout(timeout);

            int[] exec = jdbcSupport.executeBatch(sqls.toArray(new String[0]));
            for (int a : exec) {
                affected += a;
            }
        } catch (Exception ex) {
            throw new DataAccessException("Batch SQL Error! #", ex);
        }
        return affected;
    }
}