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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 触发动作/操作定义
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/23
 */
public interface TriggerAction {
	
	static final Log LOG = LogFactory.getLog(TriggerAction.class);
	
	/**
	 * 动作类型
	 * 
	 * @return
	 */
	ActionType getType();
	
	/**
	 * 源实体过滤
	 * 
	 * @param entityCode
	 * @return
	 */
	boolean isUsableSourceEntity(int entityCode);
	
	/**
	 * 操作执行
	 * 
	 * @throws TriggerException
	 */
	void execute() throws TriggerException;
}
