/*
rebuild - Building your business-systems freely.
Copyright (C) 2020 devezhao <zhaofang123@gmail.com>

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
For more information, please see <https://getrebuild.com>
*/

package com.rebuild.server.business.trigger.impl;

import com.rebuild.server.business.trigger.ActionContext;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.business.trigger.TriggerAction;
import com.rebuild.server.business.trigger.TriggerException;
import com.rebuild.server.service.OperatingContext;

/**
 * 字段回填
 *
 * @author devezhao
 * @since 2020/2/7
 */
public class FieldFillinback implements TriggerAction {

    final private ActionContext context;
    final private boolean allowNoPermissionUpdate;

    public FieldFillinback(ActionContext context) {
        this(context, Boolean.TRUE);
    }

    /**
     * @param context
     * @param allowNoPermissionUpdate
     */
    public FieldFillinback(ActionContext context, boolean allowNoPermissionUpdate) {
        this.context = context;
        this.allowNoPermissionUpdate = allowNoPermissionUpdate;
    }

    @Override
    public ActionType getType() {
        return ActionType.FIELDFILLINBACK;
    }

    @Override
    public boolean isUsableSourceEntity(int entityCode) {
        return true;
    }

    @Override
    public void execute(OperatingContext operatingContext) throws TriggerException {

    }

    @Override
    public void prepare(OperatingContext operatingContext) throws TriggerException {
    }
}
