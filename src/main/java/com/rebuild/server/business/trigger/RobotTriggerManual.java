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
     * 撤回重审触发
     *
     * @param context
     */
    public void onReapproval(OperatingContext context) {
        execAction(context, TriggerWhen.REAPPROVAL);
    }
}
