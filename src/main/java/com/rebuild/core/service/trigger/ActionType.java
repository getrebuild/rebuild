/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import com.rebuild.core.service.trigger.impl.*;
import org.apache.commons.lang.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;

/**
 * 支持的操作类型
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/23
 */
public enum ActionType {

    FIELDAGGREGATION("数据聚合", FieldAggregation.class.getName()),
    FIELDWRITEBACK("数据转写", FieldWriteback.class.getName()),
    SENDNOTIFICATION("发送通知", SendNotification.class.getName()),
    AUTOSHARE("自动共享", AutoShare.class.getName()),
    AUTOASSIGN("自动分派", AutoAssign.class.getName()),
    AUTOAPPROVAL("自动审批", AutoApproval.class.getName()),
    HOOKURL("回调 URL", "com.rebuild.rbv.trigger.HookUrl"),

    ;

    private String displayName;
    // extends TriggerAction
    private String actionClass;

    ActionType(String displayName, String actionClass) {
        this.displayName = displayName;
        this.actionClass = actionClass;
    }

    /**
     * @return
     * @deprecated Use i18n
     */
    @Deprecated
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return
     */
    public String getActionClass() {
        return actionClass;
    }

    /**
     * @param context
     * @return
     * @throws NoSuchMethodException
     */
    public TriggerAction newInstance(ActionContext context) throws ReflectiveOperationException {
        Class<?> clazz = ClassUtils.getClass(getActionClass());
        Constructor<?> c = ReflectionUtils.accessibleConstructor(clazz, ActionContext.class);
        return (TriggerAction) c.newInstance(context);
    }
}
