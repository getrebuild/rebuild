/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.integration;

import cn.devezhao.persist4j.engine.ID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NamedThreadLocal;

/**
 * @author devezhao
 * @since 26/2/7
 */
@Slf4j
public class SMSenderContextHolder {

    private static final ThreadLocal<ID> FROM_SOURCE = new NamedThreadLocal<>("Send from source");

    /**
     * @param id
     */
    public static void setFromSource(ID id) {
        FROM_SOURCE.set(id);
    }

    /**
     * @return
     */
    public static ID getFromSourceOnce() {
        ID s = FROM_SOURCE.get();
        if (s != null) FROM_SOURCE.remove();
        return s;
    }
}
