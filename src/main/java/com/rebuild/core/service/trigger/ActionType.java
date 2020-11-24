/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import com.rebuild.core.service.trigger.impl.AutoApproval;
import com.rebuild.core.service.trigger.impl.AutoAssign;
import com.rebuild.core.service.trigger.impl.AutoShare;
import com.rebuild.core.service.trigger.impl.FieldAggregation;
import com.rebuild.core.service.trigger.impl.FieldWriteback;
import com.rebuild.core.service.trigger.impl.HookUrl;
import com.rebuild.core.service.trigger.impl.SendNotification;
import org.springframework.cglib.core.ReflectUtils;

import java.lang.reflect.Constructor;

/**
 * 支持的操作类型
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/23
 */
public enum ActionType {

    FIELDAGGREGATION("数据聚合", FieldAggregation.class),
    FIELDWRITEBACK("数据转写", FieldWriteback.class),
    SENDNOTIFICATION("发送通知", SendNotification.class),
    AUTOSHARE("自动共享", AutoShare.class),
    AUTOASSIGN("自动分派", AutoAssign.class),
    AUTOAPPROVAL("自动审批", AutoApproval.class),
    HOOKURL("回调 URL", HookUrl.class),

    ;

    private String displayName;
    private Class<? extends TriggerAction> actionClazz;

    ActionType(String displayName, Class<? extends TriggerAction> actionClazz) {
        this.displayName = displayName;
        this.actionClazz = actionClazz;
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
    public Class<? extends TriggerAction> getActionClazz() {
        return actionClazz;
    }

    /**
     * @param context
     * @return
     * @throws NoSuchMethodException
     */
    public TriggerAction newInstance(ActionContext context) throws NoSuchMethodException {
        Constructor<? extends TriggerAction> c = getActionClazz().getConstructor(ActionContext.class);
        return (TriggerAction) ReflectUtils.newInstance(c, new Object[]{context});
    }
}
