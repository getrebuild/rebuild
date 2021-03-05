/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.general.OperatingObserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 触发器
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/27
 */
public class RobotTriggerObserver extends OperatingObserver {

    private static final ThreadLocal<OperatingContext> TRIGGER_SOURCE = new ThreadLocal<>();

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

    // 删除做特殊处理

    private static final Map<ID, TriggerAction[]> DELETE_ACTION_HOLDS = new ConcurrentHashMap<>();

    @Override
    protected void onDeleteBefore(OperatingContext context) {
        final ID primary = context.getAnyRecord().getPrimary();

        TriggerAction[] actionsOnDelete = RobotTriggerManager.instance.getActions(primary, TriggerWhen.DELETE);
        for (TriggerAction action : actionsOnDelete) {
            try {
                action.prepare(context);
            } catch (Exception ex) {
                LOG.error("Preparing trigger failure: " + action, ex);
            }
        }
        DELETE_ACTION_HOLDS.put(primary, actionsOnDelete);
    }

    @Override
    protected void onDelete(OperatingContext context) {
        final ID primary = context.getAnyRecord().getPrimary();
        try {
            execAction(context, TriggerWhen.DELETE);
        } finally {
            DELETE_ACTION_HOLDS.remove(primary);
        }
    }

    /**
     * 执行触发内容
     *
     * @param context
     * @param when
     */
    protected void execAction(OperatingContext context, TriggerWhen when) {
        final ID primary = context.getAnyRecord().getPrimary();

        TriggerAction[] actions = when == TriggerWhen.DELETE ?
                DELETE_ACTION_HOLDS.get(primary) : RobotTriggerManager.instance.getActions(getEffectedId(context), when);
        if (actions == null || actions.length == 0) {
            return;
        }

        final boolean cleanTriggerSource = getTriggerSource() == null;
        // 自己触发自己，避免无限执行
        if (!cleanTriggerSource
                && getTriggerSource().getAnyRecord().getPrimary().equals(primary)) {
            return;
        }

        // 设置触发源
        TRIGGER_SOURCE.set(context);

        try {
            for (TriggerAction action : actions) {
                LOG.info("Trigger [ {} ] executing on record ({}) : {}", action.getType(), when.name(), primary);

                try {
                    action.execute(context);
                } catch (Exception ex) {
                    LOG.error("Failed triggers : " + action + " << " + context, ex);
                    throw ex;
                } finally {
                    if (cleanTriggerSource) {
                        action.clean();
                    }
                }
            }

        } finally {
            if (cleanTriggerSource) {
                TRIGGER_SOURCE.remove();
            }
        }
    }

    /**
     * 获取实际影响的记录。
     * 例如在共享时传入的 Record 是 ShareAccess，而实际影响的是其中的 recordId 记录
     *
     * @return
     */
    private ID getEffectedId(OperatingContext context) {
        ID effectId = context.getAnyRecord().getPrimary();
        if (effectId.getEntityCode() == EntityHelper.ShareAccess) {
            effectId = context.getAnyRecord().getID("recordId");
        }
        return effectId;
    }

    /**
     * 获取当前（线程）触发源（如有）
     *
     * @return
     */
    public static OperatingContext getTriggerSource() {
        return TRIGGER_SOURCE.get();
    }
}
