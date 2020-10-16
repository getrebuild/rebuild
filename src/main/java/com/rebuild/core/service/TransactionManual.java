/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import com.rebuild.core.Application;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * 手动事物管理
 *
 * @author devezhao
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
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
