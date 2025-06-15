/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.googlecode.aviator.exception.StandardError;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.bizz.InternalPermission;
import com.rebuild.core.service.SafeObservable;
import com.rebuild.core.service.TransactionManual;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.general.OperatingObserver;
import com.rebuild.core.service.general.RepeatedRecordsException;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.service.trigger.impl.FieldAggregation;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.CommonsLog;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.web.KnownExceptionConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.NamedThreadLocal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.rebuild.core.support.CommonsLog.TYPE_TRIGGER;

/**
 * 触发器
 *
 * @author devezhao
 * @since 2019/05/27
 */
@Slf4j
public class RobotTriggerObserver extends OperatingObserver {

    private static final ThreadLocal<TriggerSource> TRIGGER_SOURCE = new NamedThreadLocal<>("Trigger source");

    private static final ThreadLocal<Boolean> LAZY_TRIGGERS = new NamedThreadLocal<>("Lazy triggers");
    private static final ThreadLocal<List<Object>> LAZY_TRIGGERS_CTX = new NamedThreadLocal<>("Lazy triggers ctx");

    private static final ThreadLocal<String> ALLOW_TRIGGERS_ON_NODEAPPROVED = new NamedThreadLocal<>("Allow triggers on node-approve");

    // 少量触发器日志
    public static final boolean _TriggerLessLog = CommandArgs.getBoolean(CommandArgs._TriggerLessLog);

    @Override
    public int getOrder() {
        return 4;
    }

    @Override
    public void update(final SafeObservable o, Object context) {
        // fix: v3.7.1 预处理不能延后
        if (context instanceof OperatingContext
                && ((OperatingContext) context).getAction() == InternalPermission.DELETE_BEFORE) {
            super.update(o, context);
            return;
        }

        if (isLazyTriggers(false)) {
            List<Object> ctx = LAZY_TRIGGERS_CTX.get();
            if (ctx == null) ctx = new ArrayList<>();
            ctx.add(context);
            LAZY_TRIGGERS_CTX.set(ctx);
            if (CommonsUtils.DEVLOG) System.out.println("[dev] Lazy triggers : " + ctx);
        } else {
            super.update(o, context);
        }
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
        final ID primary = context.getFixedRecordId();

        TriggerAction[] deleteActions = RobotTriggerManager.instance.getActions(primary, TriggerWhen.DELETE);
        for (TriggerAction action : deleteActions) {
            try {
                action.prepare(context);
            } catch (Exception ex) {
                // DataValidate 直接抛出
                if (ex instanceof DataValidateException) throw ex;

                log.error("Preparing context of trigger fails : {}", action, ex);
            }
        }
        DELETE_BEFORE_HOLD.put(primary, deleteActions);
    }

    @Override
    protected void onDelete(OperatingContext context) {
        final ID primary = context.getFixedRecordId();
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
        final ID primaryId = context.getFixedRecordId();

        TriggerAction[] beExecuted = when == TriggerWhen.DELETE
                ? DELETE_BEFORE_HOLD.get(primaryId)
                : RobotTriggerManager.instance.getActions(context.getFixedRecordId(), when);
        if (beExecuted == null || beExecuted.length == 0) return;

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
            // v3.1-b5
            triggerSource.addNext(context, when);
        }

        final String sourceId = triggerSource.getSourceId();
        try {
            for (TriggerAction action : beExecuted) {
                // v3.7 审批节点触发
                if (when == TriggerWhen.APPROVED) {
                    if (!allowWhenApproved(action, context)) continue;
                }
                // v3.7 指定字段通用化
                if (when == TriggerWhen.UPDATE) {
                    JSONArray whenUpdateFields = ((JSONObject) action.getActionContext().getActionContent())
                            .getJSONArray("whenUpdateFields");
                    if (whenUpdateFields != null && !whenUpdateFields.isEmpty()) {
                        boolean hasUpdated = false;
                        for (String field : context.getAfterRecord().getAvailableFields()) {
                            if (whenUpdateFields.contains(field)) {
                                hasUpdated = true;
                                break;
                            }
                        }
                        if (!hasUpdated) continue;
                    }
                }

                int t = triggerSource.incrTriggerTimes();
                String w = String.format("Trigger.%s.%d [ %s ] executing on record (%s) : %s", sourceId, t, action, when, primaryId);

                // v4.1 延迟执行（原始触发源才需异步）
                if (originTriggerSource && action.isAsyncMode()) {
                    if (!_TriggerLessLog) log.info("[ASYNC_MODE] {}", w);

                    final ID currentUser2 = UserContextHolder.getUser();
                    final TriggerSource triggerSource2 = triggerSource;
                    TransactionManual.registerAfterCommit(() -> {
                        UserContextHolder.setUser(currentUser2);
                        TRIGGER_SOURCE.set(triggerSource2);
                        try {
                            execActionInternal(context, action, true, w);
                        } finally {
                            UserContextHolder.clearUser();
                            if (!_TriggerLessLog) log.info("Clear trigger-source : {}", getTriggerSource());
                            TRIGGER_SOURCE.remove();
                        }
                    });

                } else {
                    if (!_TriggerLessLog) log.info(w);
                    execActionInternal(context, action, originTriggerSource, w);
                }
            }

        } finally {
            if (originTriggerSource) {
                if (!_TriggerLessLog) log.info("Clear trigger-source : {}", getTriggerSource());
                TRIGGER_SOURCE.remove();
            }
        }
    }

    private void execActionInternal(OperatingContext context, TriggerAction action, boolean o, String w) {
        try {
            Object res = action.execute(context);

            boolean hasAffected = res instanceof TriggerResult && ((TriggerResult) res).hasAffected();
            if (CommonsUtils.DEVLOG) System.out.println("[dev] " + w + " > " + (res == null ? "N" : res) + (hasAffected ? " < REALLY AFFECTED" : ""));

            if (res instanceof TriggerResult) {
                if (o) {
                    ((TriggerResult) res).setChain(getTriggerSource());
                }

                CommonsLog.createLog(TYPE_TRIGGER,
                        context.getOperator(), action.getActionContext().getConfigId(), res.toString());
            }

        } catch (Throwable ex) {

            // DataValidate 直接抛出
            if (ex instanceof DataValidateException) throw ex;
            // throw of Aviator 抛出
            //noinspection ConstantValue
            if (ex instanceof StandardError) throw new DataValidateException(ex.getLocalizedMessage());

            log.error("Trigger execution failed : {} << {}", action, context, ex);
            CommonsLog.createLog(TYPE_TRIGGER,
                    context.getOperator(), action.getActionContext().getConfigId(), ex);

            // FIXME 触发器执行失败是否抛出
            if (ex instanceof TriggerException) {
                throw (TriggerException) ex;
            } else {
                String errMsg = KnownExceptionConverter.convert2ErrorMsg(ex);
                if (errMsg == null) errMsg = ex.getLocalizedMessage();
                if (ex instanceof RepeatedRecordsException) errMsg = Language.L("存在重复记录");
                if (StringUtils.isBlank(errMsg)) errMsg = ex.getClass().getSimpleName().toUpperCase();

                errMsg = Language.L("触发器执行失败 : %s", errMsg);

                ID errTrigger = action.getActionContext().getConfigId();
                errMsg = errMsg + " (" + FieldValueHelper.getLabelNotry(errTrigger) + ")";

                log.error(errMsg, ex);
                throw new TriggerException(errMsg);
            }

        } finally {
            action.clean();

            // 原始触发源则清理
            if (o) {
                FieldAggregation.cleanTriggerChain();
            }
        }
    }

    private boolean allowWhenApproved(TriggerAction action, OperatingContext context) {
        // 节点触发
        String allowTriggers = ALLOW_TRIGGERS_ON_NODEAPPROVED.get();
        if (allowTriggers != null) {
            return allowTriggers.contains(action.actionContext.getConfigId().toString());
        }

        // 最终触发
        final JSONArray whenApproveNodes = ((JSONObject) action.getActionContext().getActionContent())
                .getJSONArray("whenApproveNodes");
        // 无指定步骤
        if (whenApproveNodes == null || whenApproveNodes.isEmpty()) return true;

        ID approveRecordId = context.getFixedRecordId();
        if (MetadataHelper.isDetailEntity(approveRecordId.getEntityCode())) {
            approveRecordId = QueryHelper.getMainIdByDetail(approveRecordId);
        }

        Object[] state = Application.getQueryFactory().unique(
                approveRecordId, EntityHelper.ApprovalState, EntityHelper.ApprovalStepNodeName);
        int approvalState = (int) state[0];
        String nodeName = (String) state[1];
        if (approvalState == ApprovalState.APPROVED.getState()) {
            return (whenApproveNodes.contains(nodeName) || whenApproveNodes.contains("*"));
        }

        return true;
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
     * 延迟触发器的执行
     */
    public static void setLazyTriggers() {
        LAZY_TRIGGERS.set(true);
    }

    /**
     * 是否延迟触发器执行
     *
     * @param once
     * @return
     */
    public static boolean isLazyTriggers(boolean once) {
        Boolean is = LAZY_TRIGGERS.get();
        if (is != null && once) LAZY_TRIGGERS.remove();
        return is != null && is;
    }

    /**
     * 延迟执行触发器
     *
     * @param o
     * @return
     */
    public static int executeLazyTriggers(final SafeObservable o) {
        isLazyTriggers(true);

        List<Object> ctx = LAZY_TRIGGERS_CTX.get();
        if (ctx == null) return 0;
        LAZY_TRIGGERS_CTX.remove();

        if (!_TriggerLessLog) log.info("Will execute lazy triggers : {}", ctx);
        RobotTriggerObserver observer = new RobotTriggerObserver();
        for (Object context : ctx) {
            observer.update(o, context);
        }
        return ctx.size();
    }

    /**
     * 设置允许触发的触发器（ID）
     *
     * @param triggerIds eg. [xxx,xxx,xxx]
     */
    public static void setAllowTriggersOnNodeApproved(String triggerIds) {
        ALLOW_TRIGGERS_ON_NODEAPPROVED.set(triggerIds);
    }

    /**
     */
    public static void clearAllowTriggersOnNodeApproved() {
        ALLOW_TRIGGERS_ON_NODEAPPROVED.remove();
    }
}
