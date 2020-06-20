/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
	default boolean isUsableSourceEntity(int entityCode) {
	    return true;
    }
	
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
	 * 异步执行。注意：由于异步执行可能会开启新事物，所以如果依赖主事物的数据，可能存在脏读
	 *
	 * @return
	 */
	default boolean useAsync() {
		return false;
	}

	/**
	 * 执行后进行清理工作。注意：只有同步任务并且不是用新事物的任务才会触发
	 */
	default void clean() {}
}
