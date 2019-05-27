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

import com.rebuild.server.business.robot.trigger.CountsSlave;
import com.rebuild.server.business.robot.trigger.SendNotification;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/24
 */
public class ActionFactory {
	
	/**
	 * @return
	 */
	public static ActionType[] getAvailableOperators() {
		return new ActionType[] { ActionType.COUNTSSLAVE, ActionType.SENDNOTIFICATION };
	}
	
	/**
	 * @param type
	 * @return
	 */
	public static TriggerAction createOperator(String type) {
		return createOperator(type, null);
	}
	
	/**
	 * @param type
	 * @param context
	 * @return
	 */
	public static TriggerAction createOperator(String type, ActionContext context) {
		return createOperator(ActionType.valueOf(type), null);
	}
	
	/**
	 * @param type
	 * @param context
	 * @return
	 */
	public static TriggerAction createOperator(ActionType type, ActionContext context) {
		if (type == ActionType.COUNTSSLAVE) {
			return new CountsSlave(context);
		} else if (type == ActionType.SENDNOTIFICATION) {
			return new SendNotification(context);
		}
		throw new TriggerException("未知的预定义触发类型");
	}
}
