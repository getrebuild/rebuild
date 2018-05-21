/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.server.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.datasource.DataSourceUtils;

import cn.devezhao.persist4j.DataAccessException;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.JdbcSupport;
import cn.devezhao.persist4j.engine.StatementCallback;
import cn.devezhao.persist4j.util.SqlHelper;

/**
 * SQL执行者
 * 
 * @author <a href="mailto:zhaofang123@gmail.com">Zhao Fangfang</a>
 * @since 0.1, Mar 31, 2010
 * @version $Id: SQLExecutor.java 3563 2017-08-16 10:36:47Z devezhao $
 */
public class SqlExecutor {

	private static final int MAX_BATCH_SIZE = 100;
	
	final private PersistManagerFactory factory;
	
	/**
	 * @param factory
	 */
	protected SqlExecutor(PersistManagerFactory factory) {
		this.factory = factory;
	}
	
	/**
	 * @param sql
	 * @return
	 */
	public int execute(final String sql) {
		return execute(sql, 60);
	}
	
	/**
	 * @param sql
	 * @param timeout
	 * @return
	 */
	public int execute(final String sql, int timeout) {
		try {
			final JdbcSupport jdbcSupport = (JdbcSupport) factory.createPersistManager();
			jdbcSupport.setTimeout(timeout);
			
			return jdbcSupport.execute(new StatementCallback() {
				@Override
				public Object doInParameters(PreparedStatement pstmt) throws SQLException {
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
	 * @param sql
	 * @return
	 */
	public long executeInsert(final String sql) {
		Connection connect = DataSourceUtils.getConnection(factory.getDataSource());
		
		PreparedStatement pstmt = null;
		ResultSet keyRs = null;
		try {
			pstmt = connect.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			pstmt.executeUpdate();
			keyRs = pstmt.getGeneratedKeys();
			
			if (keyRs.next()) {
				return keyRs.getLong(1);
			} else {
				return 0;
			}
		} catch (Exception ex) {
			throw new DataAccessException("SQL#: " + sql, ex);
		} finally {
			SqlHelper.close(keyRs);
			SqlHelper.close(pstmt);
			SqlHelper.release(connect, factory.getDataSource());
		}
	}
	
	/**
	 * @param sqls
	 * @return
	 */
	public int executeBatch(final String[] sqls) {
		return executeBatch(sqls, 60);
	}
	
	/**
	 * @param sqls
	 * @param timeout
	 * @return
	 */
	public int executeBatch(final String[] sqls, int timeout) {
	    int execTotal = 0;
        List<String> tmp = new ArrayList<String>();
        for (String s : sqls) {
            tmp.add(s);
            if (tmp.size() == MAX_BATCH_SIZE) {
                try {
                    final JdbcSupport jdbcSupport = (JdbcSupport) factory.createPersistManager();
                    jdbcSupport.setTimeout(timeout);
                    
                    int[] exec = jdbcSupport.executeBatch(tmp.toArray(new String[tmp.size()]));
                    for (int a : exec) execTotal += a;
                } catch (Exception ex) {
                    throw new DataAccessException("Batch SQL Error! #", ex);
                }
                tmp.clear();
            }
        }
        
        if (!tmp.isEmpty()) {
            try {
                final JdbcSupport jdbcSupport = (JdbcSupport) factory.createPersistManager();
                jdbcSupport.setTimeout(timeout);
                
                int[] exec = jdbcSupport.executeBatch(tmp.toArray(new String[tmp.size()]));
                for (int a : exec) execTotal += a;
            } catch (Exception ex) {
                throw new DataAccessException("Batch SQL Error! #", ex);
            }
        }
        return execTotal;
	}
}