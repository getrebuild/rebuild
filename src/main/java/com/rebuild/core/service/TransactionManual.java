/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import com.rebuild.core.Application;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 手动事物管理。默认事务管理见 `application-bean.xml`
 *
 * @author devezhao
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 * @since 2019/8/21
 */
public class TransactionManual {

    /**
     * 开启一个事物
     *
     * @return
     * @see #commit(TransactionStatus)
     * @see #rollback(TransactionStatus)
     */
    public static TransactionStatus newTransaction() {
        DefaultTransactionAttribute attr = new DefaultTransactionAttribute();
        attr.setName("rb-txm-" + RandomStringUtils.randomNumeric(12));
        return getTxManager().getTransaction(attr);
    }

    /**
     * 提交
     *
     * @param status
     */
    public static void commit(TransactionStatus status) {
        getTxManager().commit(status);
        status.flush();
    }

    /**
     * 回滚
     *
     * @param status
     */
    public static void rollback(TransactionStatus status) {
        getTxManager().rollback(status);
        status.flush();
    }

    /**
     * 获取事物管理器
     *
     * @return
     */
    protected static DataSourceTransactionManager getTxManager() {
        return Application.getBean(DataSourceTransactionManager.class);
    }

    /**
     * Shadow for `TransactionAspectSupport#currentTransactionStatus`
     *
     * @return
     */
    public static TransactionStatus currentTransactionStatus() {
        return TransactionAspectSupport.currentTransactionStatus();
    }

    /**
     * Shadow for `TransactionSynchronizationManager.getCurrentTransactionName`
     *
     * @return
     */
    public static String currentTransactionName() {
        return TransactionSynchronizationManager.getCurrentTransactionName();
    }
}
