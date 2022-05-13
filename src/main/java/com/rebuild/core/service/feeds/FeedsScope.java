/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.feeds;

import cn.devezhao.persist4j.engine.ID;

/**
 * 可见范围
 *
 * @author devezhao
 * @since 2019/11/4
 */
public enum FeedsScope {

    ALL("公开"),
    SELF("私密"),
    GROUP("团队"),

    ;

    final private String name;

    FeedsScope(String name) {
        this.name = name;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param any
     * @return
     */
    public static FeedsScope parse(String any) {
        if (ID.isId(any)) {
            return GROUP;
        }
        for (FeedsScope s : values()) {
            if (any.equalsIgnoreCase(s.name())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown scope : " + any);
    }
}
