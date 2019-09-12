/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper.state;

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
