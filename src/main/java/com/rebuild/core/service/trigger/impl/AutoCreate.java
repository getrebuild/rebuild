/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerException;

/**
 * @author devezhao
 * @since 2021/3/30
 */
public class AutoCreate implements TriggerAction {

    public ActionType getType() {
        return ActionType.AUTOCREATE;
    }

    @Override
    public void execute(OperatingContext operatingContext) throws TriggerException {
        throw new UnsupportedOperationException("TODO 自动创建");
    }
}
