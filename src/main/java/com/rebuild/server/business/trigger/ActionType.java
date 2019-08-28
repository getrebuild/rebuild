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

import com.rebuild.server.business.trigger.impl.AutoAssign;
import com.rebuild.server.business.trigger.impl.AutoShare;
import com.rebuild.server.business.trigger.impl.FieldAggregation;
import com.rebuild.server.business.trigger.impl.SendNotification;
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
	SENDNOTIFICATION("发送通知 (内部消息)", SendNotification.class),

	AUTOSHARE("自动共享", AutoShare.class),
	AUTOASSIGN("自动分派", AutoAssign.class),

	;
	
	private String displayName;
	private Class<? extends TriggerAction> actionClazz;

	ActionType(String displayName, Class<? extends TriggerAction> actionClazz) {
		this.displayName = displayName;
		this.actionClazz = actionClazz;
	}

	/**
	 * @return
	 */
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
		Constructor c = getActionClazz().getConstructor(ActionContext.class);
		return (TriggerAction) ReflectUtils.newInstance(c, new Object[] { context });
	}
}
