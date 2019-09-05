/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.helper.dev;

import org.apache.commons.lang.ClassUtils;

/**
 * 状态辅助类
 *
 * @author ZHAO
 * @since 2019/9/5
 */
public class StateHelper {

    /**
     * 验证是否有效状态类
     *
     * @param clazzName
     * @return
     */
    public static boolean isStateClass(String clazzName) {
        return forName(clazzName) != null;
    }

    /**
     * 加载状态枚举
     *
     * @param clazzName
     * @return
     */
    public static Class<?> forName(String clazzName) {
        Class<?> stateEnum = null;
        try {
            stateEnum = ClassUtils.getClass(clazzName);
            if (!(stateEnum.isEnum() && ClassUtils.isAssignable(stateEnum, StateSpec.class))) {
                stateEnum = null;
            }
        } catch (ClassNotFoundException ignored) {
        }
        return stateEnum;
    }

    /**
     * @param clazzName
     * @param state
     * @return
     */
    public static StateSpec valueOf(String clazzName, int state) {
        return valueOf(forName(clazzName), state);
    }

    /**
     * @param clazzName
     * @param name
     * @return
     */
    public static StateSpec valueOf(String clazzName, String name) {
        return valueOf(forName(clazzName), name);
    }

    /**
     * @param clazz
     * @param state
     * @return
     */
    public static StateSpec valueOf(Class<?> clazz, int state) {
        assert clazz != null;
        Object[] constants = clazz.getEnumConstants();
        for (Object c : constants) {
            if (((StateSpec) c).getState() == state) {
                return (StateSpec) c;
            }
        }
        return null;
    }

    /**
     * @param clazz
     * @param name
     * @return
     */
    public static StateSpec valueOf(Class<?> clazz, String name) {
        assert clazz != null;
        Object[] constants = clazz.getEnumConstants();
        for (Object c : constants) {
            if (((StateSpec) c).getName().equals(name) || ((Enum<?>) c).name().equals(name)) {
                return (StateSpec) c;
            }
        }
        return null;
    }
}
