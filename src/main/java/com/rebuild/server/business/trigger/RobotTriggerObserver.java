/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.trigger;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.configuration.RobotTriggerManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.OperatingObserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

        final boolean cleanSource = getTriggerSource() == null;
        // 设置原始触发源
        if (cleanSource) {
            setTriggerSource(context);
        }
        // 自己触发自己，避免无限执行
        else if (getTriggerSource().getAnyRecord().getPrimary().equals(primary)) {
            return;
        }

        try {
            for (TriggerAction action : actions) {
                LOG.info("Trigger [ " + action.getType() + " ] by record : " + primary);

                try {
                    action.execute(context);
                } catch (Exception ex) {
                    LOG.error("Failed triggers : " + action + " << " + context, ex);
                    throw ex;
                } finally {
                    if (cleanSource) {
                        action.clean();
                    }
                }
            }

        } finally {
            if (cleanSource) {
                setTriggerSource(null);
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

    // -- 当前线程触发源

    private static final ThreadLocal<OperatingContext> TRIGGER_SOURCE = new ThreadLocal<>();

    /**
     * 设置触发器触发源，供其他功能使用
     *
     * @param source
     */
    private static void setTriggerSource(OperatingContext source) {
        if (source == null) {
            TRIGGER_SOURCE.remove();
        } else {
            TRIGGER_SOURCE.set(source);
        }
    }

    /**
     * 获取当前（线程）触发源（如有），即原始触发记录
     *
     * @return
     */
    public static OperatingContext getTriggerSource() {
        return TRIGGER_SOURCE.get();
    }
}
