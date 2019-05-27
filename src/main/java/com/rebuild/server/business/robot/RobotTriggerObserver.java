/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.robot;

import com.rebuild.server.configuration.RobotTriggerManager;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.OperatingObserver;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/27
 */
public class RobotTriggerObserver extends OperatingObserver {
	
	@Override
	protected void onCreate(OperatingContext context) {
		execAction(context, TriggerWhen.CREATE);
	}
	
	@Override
	protected void onUpdate(OperatingContext context) {
		execAction(context, TriggerWhen.UPDATE);
	}
	
	@Override
	protected void onDelete(OperatingContext context) {
		execAction(context, TriggerWhen.DELETE);
	}
	
	@Override
	protected void onAssign(OperatingContext context) {
		execAction(context, TriggerWhen.ASSIGN);
	}
	
	@Override
	protected void onShare(OperatingContext context) {
		execAction(context, TriggerWhen.SHARE);
	}
	
	@Override
	protected void onUnshare(OperatingContext context) {
		execAction(context, TriggerWhen.UNSHARE);
	}
	
	/**
	 * @param context
	 * @param when
	 */
	protected void execAction(OperatingContext context, TriggerWhen when) {
		TriggerAction[] actions = RobotTriggerManager.instance.getActions(context.getAnyRecord().getPrimary(), TriggerWhen.CREATE);
		if (actions == null || actions.length == 0) {
			return;
		}
		for (TriggerAction action : actions) {
			try {
				action.execute();
			} catch (Exception ex) {
				LOG.error("Executing trigger failure: " + action, ex);
			}
		}
	}
}
