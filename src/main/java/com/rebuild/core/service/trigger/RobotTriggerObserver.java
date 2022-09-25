/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.general.OperatingObserver;
import com.rebuild.core.service.general.RepeatedRecordsException;
import com.rebuild.core.service.trigger.impl.FieldAggregation;
import com.rebuild.core.support.CommonsLog;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NamedThreadLocal;

import java.util.Map;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;

import static com.rebuild.core.support.CommonsLog.TYPE_TRIGGER;

/**
 * 触发器
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/27
 */
@Slf4j
public class RobotTriggerObserver extends OperatingObserver {

    private static final ThreadLocal<TriggerSource> TRIGGER_SOURCE = new NamedThreadLocal<>("Trigger source");

    private static final ThreadLocal<Boolean> SKIP_TRIGGERS = new NamedThreadLocal<>("Skip triggers");

    @Override
    public void update(final Observable o, Object arg) {
        if (isSkipTriggers(false)) return;
        super.update(o, arg);
    }

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

    private static final Map<ID, TriggerAction[]> DELETE_BEFORE_HOLD = new ConcurrentHashMap<>();

    @Override
    protected void onDeleteBefore(OperatingContext context) {
        final ID primary = context.getAnyRecord().getPrimary();

        TriggerAction[] deleteActions = RobotTriggerManager.instance.getActions(primary, TriggerWhen.DELETE);
        for (TriggerAction action : deleteActions) {
            try {
                action.prepare(context);
            } catch (Exception ex) {
                // DataValidate 直接抛出
                if (ex instanceof DataValidateException) throw ex;

                log.error("Preparing context of trigger failed : {}", action, ex);
            }
        }
        DELETE_BEFORE_HOLD.put(primary, deleteActions);
    }

    @Override
    protected void onDelete(OperatingContext context) {
        final ID primary = context.getAnyRecord().getPrimary();
        try {
            execAction(context, TriggerWhen.DELETE);
        } finally {
            DELETE_BEFORE_HOLD.remove(primary);
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
                ? DELETE_BEFORE_HOLD.get(primaryId)
                : RobotTriggerManager.instance.getActions(getRealRecordId(context), when);
        if (beExecuted == null || beExecuted.length == 0) {
            return;
        }

        TriggerSource triggerSource = getTriggerSource();
        final boolean originTriggerSource = triggerSource == null;

        // 设置原始触发源
        if (originTriggerSource) {
            TRIGGER_SOURCE.set(new TriggerSource(context, when));
            triggerSource = getTriggerSource();

            // 强制清理一次，正常不会出现此情况
            Object o = FieldAggregation.cleanTriggerChain();
            if (o != null) log.warn("Force clean last trigger-chain : {}", o);

        } else {
            // 是否自己触发自己，避免无限执行
            boolean isOriginRecord = primaryId.equals(triggerSource.getOriginRecord());

            String lastKey = triggerSource.getLastSourceKey();
            triggerSource.addNext(context, when);
            String currentKey = triggerSource.getLastSourceKey();

            if (isOriginRecord && lastKey.equals(currentKey)) {
                if (!triggerSource.isSkipOnce()) {
                    log.warn("Self trigger, ignore : {} < {}", currentKey, lastKey);
                    return;
                }
            }

            // FIXME 20220811 此处的判断可能不需要，因为有 `trigger-chain`
        }

        final String sourceId = triggerSource.getSourceId();
        try {
            for (TriggerAction action : beExecuted) {
                String w = String.format("Trigger.%s [ %s ] executing on record (%s) : %s",
                        sourceId, action.getType(), when.name(), primaryId);
                log.info(w);

                try {
                    Object ret = action.execute(context);
                    System.out.println("[dev] " + w + " > " + (ret == null ? "N" : ret));

                    String logContent = ret == null ? null : ret.toString();
                    if (originTriggerSource) {
                        if (logContent != null) logContent += "; ";
                        logContent += "chain:" + getTriggerSource();
                    }
                    CommonsLog.createLog(TYPE_TRIGGER,
                            context.getOperator(), action.getActionContext().getConfigId(), logContent);

                } catch (Throwable ex) {

                    // DataValidate 直接抛出
                    if (ex instanceof DataValidateException) throw ex;

                    log.error("Trigger execution failed : {} << {}", action, context, ex);
                    CommonsLog.createLog(TYPE_TRIGGER,
                            context.getOperator(), action.getActionContext().getConfigId(), ex);

                    // FIXME 触发器执行失败是否抛出
                    if (ex instanceof TriggerException) {
                        throw (TriggerException) ex;
                    } else {
                        String errMsg = ex.getLocalizedMessage();
                        if (ex instanceof RepeatedRecordsException) errMsg = Language.L("存在重复记录");

                        errMsg = Language.L("触发器执行失败 : %s", errMsg);
                        ID lastTrigger = FieldAggregation.getLastTrigger();
                        if (lastTrigger != null) {
                            errMsg = errMsg + " (" + FieldValueHelper.getLabelNotry(lastTrigger) + ")";
                        }

                        throw new TriggerException(errMsg);
                    }

                } finally {
                    action.clean();

                    // 原始触发源则清理
                    if (originTriggerSource) {
                        FieldAggregation.cleanTriggerChain();
                    }
                }
            }

        } finally {
            if (originTriggerSource) {
                log.info("Clear trigger-source : {}", getTriggerSource());
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
    private ID getRealRecordId(OperatingContext context) {
        ID recordId = context.getAnyRecord().getPrimary();
        if (recordId.getEntityCode() == EntityHelper.ShareAccess) {
            recordId = context.getAnyRecord().getID("recordId");
        }
        return recordId;
    }

    // --

    /**
     * 获取当前（线程）触发源（如有）
     *
     * @return
     */
    public static TriggerSource getTriggerSource() {
        return TRIGGER_SOURCE.get();
    }

    /**
     * 跳过触发器的执行
     */
    public static void setSkipTriggers() {
        SKIP_TRIGGERS.set(true);
    }

    /**
     * @param once
     * @return
     * @see #setSkipTriggers()
     */
    public static boolean isSkipTriggers(boolean once) {
        Boolean is = SKIP_TRIGGERS.get();
        if (is != null && once) SKIP_TRIGGERS.remove();
        return is != null && is;
    }
}
