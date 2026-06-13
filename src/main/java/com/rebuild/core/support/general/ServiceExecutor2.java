/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.TransactionManual;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.support.Executor2;

/**
 * @author ZHAO
 * @see GeneralEntityServiceContextHolder
 * @see UserContextHolder
 * @since 2024/8/7
 */
public class ServiceExecutor2 {

    /**
     * 跳过权限执行
     *
     * @param executor
     * @param <T>
     * @return
     */
    public static <T> T withoutPrivileges(Executor2<T> executor) {
        return withoutPrivileges(executor, false);
    }

    /**
     * 跳过权限执行
     *
     * @param executor
     * @param forceUpdate 是否强制
     * @param <T>
     * @return
     */
    public static <T> T withoutPrivileges(Executor2<T> executor, boolean forceUpdate) {
        return withoutSpec(executor, true, false, forceUpdate);
    }

    /**
     * 跳过触发器执行
     *
     * @param executor
     * @param <T>
     * @return
     */
    public static <T> T withoutTriggers(Executor2<T> executor) {
        return withoutTriggers(executor, false);
    }

    /**
     * 跳过触发器执行
     *
     * @param executor
     * @param forceUpdate 是否强制
     * @param <T>
     * @return
     */
    public static <T> T withoutTriggers(Executor2<T> executor, boolean forceUpdate) {
        return withoutSpec(executor, false, true, forceUpdate);
    }

    /**
     * 跳过权限和触发器执行
     *
     * @param executor
     * @param <T>
     * @return
     */
    public static <T> T withoutPrivilegesAndTriggers(Executor2<T> executor) {
        return withoutPrivilegesAndTriggers(executor, false);
    }

    /**
     * 跳过权限和触发器执行
     *
     * @param executor
     * @param forceUpdate 是否强制
     * @param <T>
     * @return
     */
    public static <T> T withoutPrivilegesAndTriggers(Executor2<T> executor, boolean forceUpdate) {
        return withoutSpec(executor, true, true, forceUpdate);
    }

    /**
     * @param executor
     * @param withoutPrivileges
     * @param withoutTriggers
     * @param forceUpdate
     * @param <T>
     * @return
     */
    static <T> T withoutSpec(Executor2<T> executor,
                             boolean withoutPrivileges, boolean withoutTriggers, boolean forceUpdate) {
        ID keepCurrentUser = null;
        if (withoutPrivileges) keepCurrentUser = UserContextHolder.setUser(UserService.SYSTEM_USER);
        if (withoutTriggers) GeneralEntityServiceContextHolder.setQuickMode();
        if (forceUpdate) GeneralEntityServiceContextHolder.setAllowForceUpdate(EntityHelper.UNSAVED_ID);

        try {
            return executor.exec();
        } finally {
            if (keepCurrentUser != null) UserContextHolder.clearUser(keepCurrentUser);
            if (withoutTriggers) GeneralEntityServiceContextHolder.isQuickMode(true);
            if (forceUpdate) GeneralEntityServiceContextHolder.isAllowForceUpdate(true);
        }
    }

    /**
     * 当前事务完成后执行
     *
     * @param c
     * @see TransactionManual#registerAfterCommit(Runnable)
     */
    public static void execAfterCommit(Runnable c) {
        TransactionManual.registerAfterCommit(c);
    }
}
