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

    FIELDAGGREGATION("数据聚合", FieldAggregation.class),
    FIELDWRITEBACK("自动更新 (数据转写)", FieldWriteback.class),
    AUTOUPDATE("自动更新", AutoUpdate.class),
    AUTOCREATE("自动创建", AutoCreate.class),
    AUTOSHARE("自动共享", AutoShare.class),
    AUTOASSIGN("自动分派", AutoAssign.class),
    AUTOAPPROVAL("自动审批", AutoApproval.class),
    SENDNOTIFICATION("发送通知", SendNotification.class),
    AUTOTRANSFORM("自动记录转换", "com.rebuild.rbv.trigger.AutoTransform"),
    HOOKURL("回调 URL", "com.rebuild.rbv.trigger.HookUrl"),

    ;

    private String displayName;
    // extends TriggerAction
    private String actionClass;

    ActionType(String displayName, String actionClass) {
        this.displayName = displayName;
        this.actionClass = actionClass;
    }

    ActionType(String displayName, Class<? extends TriggerAction> actionClass) {
        this(displayName, actionClass.getName());
    }

    /**
     * Use i18n
     * @return
     */
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
     * @throws ReflectiveOperationException
     */
    protected TriggerAction newInstance(ActionContext context) throws ReflectiveOperationException {
        Class<?> clazz = ClassUtils.getClass(getActionClass());
        Constructor<?> c = ReflectionUtils.accessibleConstructor(clazz, ActionContext.class);
        return (TriggerAction) c.newInstance(context);
    }
}
