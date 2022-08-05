/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import com.rebuild.core.service.general.OperatingContext;

/**
 * 触发动作/操作定义。
 * 注意：如果是异步处理将没有事物，同时会丢失一些线程量（如果需要请手动设置）
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/23
 */
public abstract class TriggerAction {

    final protected ActionContext actionContext;

    protected TriggerAction(ActionContext actionContext) {
        this.actionContext = actionContext;
    }

    public ActionContext getActionContext() {
        return actionContext;
    }

    abstract public ActionType getType();

    abstract public Object execute(OperatingContext operatingContext) throws TriggerException;

    /**
     * 如果是删除动作，会先调用此方法。可在此方法中保持一些数据状态，以便删除后还可继续使用
     *
     * @param operatingContext
     * @throws TriggerException
     */
    protected void prepare(OperatingContext operatingContext) throws TriggerException {
    }

    /**
     * 执行后进行清理工作。注意：只有同步任务并且不是用新事物的任务才会触发
     */
    public void clean() {
    }

    /**
     * 实体是否可用此触发器
     *
     * @param entityCode
     * @return
     */
    public boolean isUsableSourceEntity(int entityCode) {
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "#" + actionContext.getConfigId();
    }
}
