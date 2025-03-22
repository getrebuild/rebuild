/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.stereotype.Component;

/**
 * @author devezhao
 * @since 2025/3/8
 */
@ConditionalOnMissingClass("com.rebuild.Rbv")
@Component
@Slf4j
public class RbvFunction {

    protected RbvFunction() {}

    public static RbvFunction call() {
        return Application.getBean(RbvFunction.class);
    }

    // --

    public void setWeakMode(ID id) {
        log.warn("No RbvFunction : setWeakMode");
    }

    public ID getWeakMode(boolean once) {
        log.warn("No RbvFunction : getWeakMode");
        return null;
    }
}
