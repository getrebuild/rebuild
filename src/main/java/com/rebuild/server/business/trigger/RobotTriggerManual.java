/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.trigger;

import com.rebuild.server.service.OperatingContext;

/**
 * 手动触发，而非通过 ObservableService 规范触发
 *
 * @author devezhao
 * @since 2019/11/22
 */
public class RobotTriggerManual extends RobotTriggerObserver {

    /**
     * 审批通过触发
     *
     * @param context
     */
    public void onApproved(OperatingContext context) {
        execAction(context, TriggerWhen.APPROVED);
    }

    /**
     * 审批撤销触发
     *
     * @param context
     */
    public void onRevoked(OperatingContext context) {
        execAction(context, TriggerWhen.REVOKED);
    }
}
