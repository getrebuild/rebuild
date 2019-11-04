/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.feeds;

import cn.devezhao.persist4j.engine.ID;

/**
 * 可见范围
 *
 * @author devezhao
 * @since 2019/11/4
 */
public enum FeedsScope {

    ALL("公开"),
    SELF("私有"),
    GROUP("群组"),

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
            if (any.equalsIgnoreCase(s.name())) return s;
        }
        throw new IllegalArgumentException("Unknow scope : " + any);
    }
}
