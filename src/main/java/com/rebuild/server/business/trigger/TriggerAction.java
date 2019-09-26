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

import com.rebuild.server.service.OperatingContext;

/**
 * 触发动作/操作定义。
 * 注意：如果是异步处理将没有事物，同时会丢失一些线程量
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/23
 */
public interface TriggerAction {
	
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
	 * @param operatingContext
	 * @throws TriggerException
	 */
	void execute(OperatingContext operatingContext) throws TriggerException;
	
	/**
	 * 如果是删除动作，会先调用此方法。应该在此方法中保持一些数据的状态，以便删除后还可以继续使用
	 * 
	 * @param operatingContext
	 * @throws TriggerException
	 */
	void prepare(OperatingContext operatingContext) throws TriggerException;

	/**
	 * 异步执行（异步执行会开启新事物）
	 *
	 * @return
	 * @see #useNewTransaction()
	 */
	default boolean useAsync() {
		return false;
	}

	/**
	 * 是否使用新事物执行。使用新事物不会对 <tt>主事物/主操作</tt> 产生影响
	 *
	 * @return
	 */
	default boolean useNewTransaction() {
		return false;
	}
}
