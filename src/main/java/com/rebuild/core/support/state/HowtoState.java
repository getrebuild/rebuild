/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.state;

/**
 * 演示
 *
 * @author devezhao
 * @since 09/05/2019
 */
public enum HowtoState implements StateSpec {

    DRAFT(1, "草稿"),
    PENDING(2, "处理中"),
    SOLVED(10, "已解决"),
    REJECTED(11, "已驳回"),

    ;

    final private int state;
    final private String name;

    HowtoState(int state, String name) {
        this.state = state;
        this.name = name;
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isDefault() {
        return this == SOLVED;
    }

    /**
     * @param state
     * @return
     */
    public static StateSpec valueOf(int state) {
        return StateHelper.valueOf(HowtoState.class, state);
    }
}
