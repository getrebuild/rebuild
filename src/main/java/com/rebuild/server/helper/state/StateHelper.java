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

package com.rebuild.server.helper.state;

import cn.devezhao.persist4j.Field;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
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
        try {
            return getSatetClass(clazzName) != null;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * @param stateField
     * @return
     */
    public static Class<?> getSatetClass(Field stateField) {
        if (EntityHelper.ApprovalState.equalsIgnoreCase(stateField.getName())) {
            return ApprovalState.class;
        }

        String stateClass = new EasyMeta(stateField).getFieldExtConfig().getString("stateClass");
        return getSatetClass(stateClass);
    }

    /**
     * 加载状态枚举类
     *
     * @param stateClass
     * @return
     * @throws IllegalArgumentException
     */
    public static Class<?> getSatetClass(String stateClass) throws IllegalArgumentException {
        assert stateClass != null;
        Class<?> stateEnum = null;
        try {
            stateEnum = ClassUtils.getClass(stateClass);
            if (stateEnum.isEnum() && ClassUtils.isAssignable(stateEnum, StateSpec.class)) {
                return stateEnum;
            }
        } catch (ClassNotFoundException ignored) {
            throw new IllegalArgumentException("No class of state found: " + stateClass);
        }
        throw new IllegalArgumentException("Bad class of state found: " + stateEnum);
    }

    /**
     * @param clazzName
     * @param state
     * @return
     */
    public static StateSpec valueOf(String clazzName, int state) {
        return valueOf(getSatetClass(clazzName), state);
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
}
