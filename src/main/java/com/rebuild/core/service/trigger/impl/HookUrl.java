/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import com.rebuild.core.service.trigger.ActionContext;

/**
 * 调用指定 URL
 *
 * @author ZHAO
 * @since 2020/11/25
 */
public class HookUrl extends SendNotification {

    public HookUrl(ActionContext context) {
        super(context);
    }

    @Override
    protected void executeAsync() {
    }
}
