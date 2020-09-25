/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.persist4j.engine.ID;
import org.springframework.core.NamedThreadLocal;

/**
 * 当前请求用户（线程量）
 *
 * @author zhaofang123@gmail.com
 * @since 11/20/2017
 */
public class CurrentCaller {

    private static final ThreadLocal<ID> CALLER = new NamedThreadLocal<>("Current session user");

    /**
     * @param caller
     */
    public void set(ID caller) {
        CALLER.set(caller);
    }

    /**
     *
     */
    public void clean() {
        CALLER.remove();
    }

    /**
     * @return
     */
    public ID get() {
        return get(false);
    }

    /**
     * @param allowedNull
     * @return
     * @throws AccessDeniedException If caller not found
     */
    public ID get(boolean allowedNull) throws AccessDeniedException {
        ID caller = CALLER.get();
        if (caller == null && !allowedNull) {
            throw new AccessDeniedException("无有效用户");
        }
        return caller;
    }
}
