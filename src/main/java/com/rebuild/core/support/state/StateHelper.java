/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.state;

import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.service.approval.ApprovalState;
import org.apache.commons.lang.ClassUtils;
import org.springframework.util.Assert;

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
            getSatetClass(clazzName);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * @param stateField
     * @return
     * @see #getSatetClass(String)
     */
    public static Class<?> getSatetClass(Field stateField) {
        if (EntityHelper.ApprovalState.equalsIgnoreCase(stateField.getName())) {
            return ApprovalState.class;
        }

        String stateClass = EasyMetaFactory.valueOf(stateField).getExtraAttr(EasyFieldConfigProps.STATE_CLASS);
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
        Assert.notNull(stateClass, "[stateClass] cannot be null");

        // fix V1
        if ("com.rebuild.server.helper.state.HowtoState".equalsIgnoreCase(stateClass)) {
            stateClass = HowtoState.class.getName();
        }

        Class<?> stateEnum;
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
     * @see #valueOf(Class, int)
     */
    public static StateSpec valueOf(String clazzName, int state) {
        return valueOf(getSatetClass(clazzName), state);
    }

    /**
     * @param stateClass
     * @param state
     * @return
     * @throws IllegalArgumentException
     */
    public static StateSpec valueOf(Class<?> stateClass, int state) throws IllegalArgumentException {
        Assert.notNull(stateClass, "[stateClass] cannot be null");

        Object[] constants = stateClass.getEnumConstants();
        for (Object c : constants) {
            if (((StateSpec) c).getState() == state) {
                return (StateSpec) c;
            }
        }
        throw new IllegalArgumentException("state=" + state);
    }
}
