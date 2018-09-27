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
 */
public class SqlExecutor {

	private static final int MAX_BATCH_SIZE = 100;
	
	final private PersistManagerFactory PM_FACTORY;
	
	protected SqlExecutor(PersistManagerFactory factory) {
		this.PM_FACTORY = factory;
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
	 * @param timeout
	 * @return
	 */
	public int execute(String sql, int timeout) {
		try {
			final JdbcSupport jdbcSupport = (JdbcSupport) PM_FACTORY.createPersistManager();
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
	public long executeInsert(String sql) {
		Connection connect = DataSourceUtils.getConnection(PM_FACTORY.getDataSource());
		
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
			SqlHelper.close(connect, PM_FACTORY.getDataSource());
		}
	}
	
	/**
	 * @param sqls
	 * @return
	 */
	public int executeBatch(String[] sqls) {
		return executeBatch(sqls, 60);
	}
	
	/**
	 * @param sqls
	 * @param timeout
	 * @return
	 */
	public int executeBatch(String[] sqls, int timeout) {
	    int execTotal = 0;
        List<String> tmp = new ArrayList<String>();
        for (String s : sqls) {
            tmp.add(s);
            if (tmp.size() == MAX_BATCH_SIZE) {
                try {
                    final JdbcSupport jdbcSupport = (JdbcSupport) PM_FACTORY.createPersistManager();
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
                final JdbcSupport jdbcSupport = (JdbcSupport) PM_FACTORY.createPersistManager();
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