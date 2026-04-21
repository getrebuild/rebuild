/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import com.rebuild.core.Application;
import com.rebuild.core.support.task.TaskExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 手动事物管理。默认事务管理见 `application-bean.xml`
 *
 * @author devezhao
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 * @since 2019/8/21
 */
@Slf4j
public class TransactionManual {

    /**
     * 开启一个事物（如若当前已在事物中，则使用嵌套事物）
     *
     * @return
     * @see #commit(TransactionStatus)
     * @see #rollback(TransactionStatus)
     * @see #newTransaction(boolean)
     */
    public static TransactionStatus newTransaction() {
        return newTransaction(true);
    }

    /**
     * 开启一个新事物
     *
     * @param useNested PROPAGATION_NESTED or PROPAGATION_REQUIRES_NEW
     * @return
     * @see #commit(TransactionStatus)
     * @see #rollback(TransactionStatus)
     */
    public static TransactionStatus newTransaction(boolean useNested) {
        DefaultTransactionAttribute attr = new DefaultTransactionAttribute();
        attr.setName("rbTransaction-" + RandomStringUtils.randomNumeric(12));
        if (useNested) {
            attr.setPropagationBehavior(DefaultTransactionAttribute.PROPAGATION_NESTED);
        } else {
            attr.setPropagationBehavior(DefaultTransactionAttribute.PROPAGATION_REQUIRES_NEW);
        }
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

    /**
     * 当前事务完成后回调
     *
     * @param c
     * @see TransactionSynchronizationManager#isSynchronizationActive()
     * @see TransactionSynchronizationManager#registerSynchronization(TransactionSynchronization)
     */

    /**
     * 当前事务完成后回调
     *
     * @param c
     * @return
     */
    public static void registerAfterCommit(Runnable c) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    new Thread(c).start();
                }
                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        log.debug("Transaction not committed ({}) : {}", status, c);
                    }
                }
            });
            return;
        }

        // 非事物中
        log.debug("Transaction synchronization is not active, start directly : {}", c);
        new Thread(c).start();
    }

    /**
     * 当前事务回滚后回调
     *
     * @param c
     * @return
     */
    public static boolean registerAfterRollback(Runnable c) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        new Thread(c).start();
                    }
                }
            });
            return true;
        }

        // 非事物中
        log.debug("Transaction synchronization is not active, start directly : {}", c);
        return false;
    }

    /**
     * 当前事务完成后回调。支持延迟/覆盖
     *
     * @param c
     * @param delayInMs
     * @param keyCancel
     */
    public static void registerAfterCommit(Runnable c, int delayInMs, String keyCancel) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    TaskExecutors.schedule(c, delayInMs, keyCancel);
                }
                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        log.debug("Transaction not committed ({}) : {}", status, c);
                    }
                }
            });
            return;
        }

        // 非事物中
        log.debug("Transaction synchronization is not active, start directly : {}", c);
        TaskExecutors.schedule(c, delayInMs, keyCancel);
    }

    /**
     * 是否在事物中
     *
     * @return
     */
    public static boolean isInTransaction() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /**
     * @return
     */
    public static String getTransactionInfo() {
        return "是否在事务中:" + TransactionSynchronizationManager.isActualTransactionActive() +
                "; 同步是否活跃:" + TransactionSynchronizationManager.isSynchronizationActive() +
                "; 当前事务名称:" + TransactionSynchronizationManager.getCurrentTransactionName();
    }
}
