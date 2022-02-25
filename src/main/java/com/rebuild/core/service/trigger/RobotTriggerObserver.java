/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import com.googlecode.aviator.exception.ExpressionRuntimeException;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.general.OperatingObserver;
import com.rebuild.core.service.general.RepeatedRecordsException;
import com.rebuild.core.support.i18n.Language;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 触发器
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/27
 */
@Slf4j
public class RobotTriggerObserver extends OperatingObserver {

    private static final ThreadLocal<OperatingContext> TRIGGER_SOURCE = new ThreadLocal<>();
    private static final ThreadLocal<ID> TRIGGER_SOURCE_LASTID = new ThreadLocal<>();

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
                // DataValidate 直接抛出
                if (ex instanceof DataValidateException) throw ex;

                log.error("Preparing context of trigger failed : {}", action, ex);
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
        final ID primaryId = context.getAnyRecord().getPrimary();

        TriggerAction[] beExecuted = when == TriggerWhen.DELETE
                ? DELETE_ACTION_HOLDS.get(primaryId)
                : RobotTriggerManager.instance.getActions(getEffectedId(context), when);
        if (beExecuted == null || beExecuted.length == 0) {
            return;
        }

        final boolean originalTriggerSource = getTriggerSource() == null;
        // 设置原始触发源
        if (originalTriggerSource) {
            TRIGGER_SOURCE.set(context);
        }
        // 自己触发自己，避免无限执行
        else if (primaryId.equals(getTriggerSource().getAnyRecord().getPrimary())
                || primaryId.equals(TRIGGER_SOURCE_LASTID.get())) {
            return;
        }

        TRIGGER_SOURCE_LASTID.set(primaryId);

        try {
            for (TriggerAction action : beExecuted) {
                log.info("Trigger [ {} ] executing on record ({}) : {}", action.getType(), when.name(), primaryId);

                try {
                    action.execute(context);
                } catch (Throwable ex) {
                    // DataValidate 直接抛出
                    if (ex instanceof DataValidateException) throw ex;

                    log.error("Trigger execution failed : {} << {}", action, context, ex);

                    // FIXME 触发器执行失败是否抛出
                    if (ex instanceof MissingMetaExcetion
                            || ex instanceof ExpressionRuntimeException
                            || ex instanceof RepeatedRecordsException) {
                        throw new TriggerException(Language.L("触发器执行失败 : %s", ex.getLocalizedMessage()));
                    } else if (ex instanceof TriggerException) {
                        throw (TriggerException) ex;
                    } else {
                        throw new RebuildException(ex);
                    }

                } finally {
                    if (originalTriggerSource) {
                        action.clean();
                    }
                }
            }

        } finally {
            if (originalTriggerSource) {
                TRIGGER_SOURCE.remove();
                TRIGGER_SOURCE_LASTID.remove();
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
