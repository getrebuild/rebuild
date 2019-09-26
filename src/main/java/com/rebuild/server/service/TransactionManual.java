/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * 手动事物管理
 *
 * @author devezhao
 * @since 2019/8/21
 */
public class TransactionManual {

    /**
     * 获取事物管理器
     *
     * @return
     */
    public static DataSourceTransactionManager getTxManager() {
        return Application.getBean(DataSourceTransactionManager.class);
    }

    /**
     * 开启一个事物
     *
     * @return
     */
    public static TransactionStatus newTransaction() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return getTxManager().getTransaction(def);
    }

    /**
     * 提交
     *
     * @param status
     */
    public static void commit(TransactionStatus status) {
        getTxManager().commit(status);
    }

    /**
     * 回滚
     *
     * @param status
     */
    public static void rollback(TransactionStatus status) {
        getTxManager().rollback(status);
    }

    /**
     * Shadow for TransactionAspectSupport#currentTransactionStatus
     *
     * @return
     */
    public static TransactionStatus currentTransactionStatus() {
        return TransactionAspectSupport.currentTransactionStatus();
    }
}
