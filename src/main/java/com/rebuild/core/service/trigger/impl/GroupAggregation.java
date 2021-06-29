/*
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
*/

package com.rebuild.core.service.trigger.impl;

import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerException;

/**
 * @author devezhao
 * @since 2021/6/28
 */
public class GroupAggregation implements TriggerAction {

    final protected ActionContext context;

    // 允许无权限分派
    final private boolean allowNoPermissionAssign;

    /**
     * @param context
     */
    public GroupAggregation(ActionContext context) {
        this(context, Boolean.TRUE);
    }

    /**
     * @param context
     * @param allowNoPermissionAssign
     */
    public GroupAggregation(ActionContext context, boolean allowNoPermissionAssign) {
        this.context = context;
        this.allowNoPermissionAssign = allowNoPermissionAssign;
    }

    @Override
    public ActionType getType() {
        return ActionType.GROUPAGGREGATION;
    }

    @Override
    public void execute(OperatingContext operatingContext) throws TriggerException {
    }
}
