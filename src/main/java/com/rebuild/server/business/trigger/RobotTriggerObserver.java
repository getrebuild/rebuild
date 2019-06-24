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

package com.rebuild.server.business.trigger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rebuild.server.configuration.RobotTriggerManager;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.OperatingObserver;

import cn.devezhao.persist4j.engine.ID;

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
		TriggerAction[] actions = RobotTriggerManager.instance.getActions(context.getAnyRecord().getPrimary(), when);
		for (TriggerAction action : actions) {
			try {
				action.execute(context);
			} catch (Exception ex) {
				LOG.error("Executing trigger failure: " + action, ex);
			}
		}
	}
	
	// 删除做特殊处理
	
	private static final Map<ID, TriggerAction[]> DELETE_ACTION_HOLDS = new ConcurrentHashMap<>();
	
	@Override
	protected void onDeleteBefore(OperatingContext context) {
		final ID primary = context.getAnyRecord().getPrimary();
		TriggerAction[] actions = RobotTriggerManager.instance.getActions(primary, TriggerWhen.DELETE);
		for (TriggerAction action : actions) {
			try {
				action.prepare(context);
			} catch (Exception ex) {
				LOG.error("Preparing trigger failure: " + action, ex);
			}
		}
		DELETE_ACTION_HOLDS.put(primary, actions);
	}
	
	@Override
	protected void onDelete(OperatingContext context) {
		final ID primary = context.getAnyRecord().getPrimary();
		TriggerAction[] holdActions = DELETE_ACTION_HOLDS.get(primary);
		if (holdActions == null) {
			LOG.warn("No action held for trigger of delete");
			return;
		}
		for (TriggerAction action : holdActions) {
			try {
				action.execute(context);
			} catch (Exception ex) {
				LOG.error("Executing trigger failure: " + action, ex);
			}
		}
		DELETE_ACTION_HOLDS.remove(primary);
	}
}
