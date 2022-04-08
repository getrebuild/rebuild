/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.persist4j.engine.ID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NamedThreadLocal;
import org.springframework.util.Assert;

/**
 * 跳过权限检查
 *
 * @author devezhao
 * @since 2020/9/28
 */
@Slf4j
public class PrivilegesGuardContextHolder {

    private static final ThreadLocal<ID> SKIP_GUARD = new NamedThreadLocal<>("Skip some check once");

    /**
     * 允许无权限操作一次
     *
     * @param recordId
     */
    public static void setSkipGuard(ID recordId) {
        Assert.notNull(recordId, "[recordId] cannot be null");

        ID existsWarn = SKIP_GUARD.get();
        if (existsWarn != null) {
            log.warn("Not removed skip record : {}", existsWarn);
            SKIP_GUARD.remove();
        }
        SKIP_GUARD.set(recordId);
    }

    /**
     * @return
     */
    public static ID getSkipGuardOnce() {
        ID recordId = SKIP_GUARD.get();
        if (recordId != null) SKIP_GUARD.remove();
        return recordId;
    }
}
