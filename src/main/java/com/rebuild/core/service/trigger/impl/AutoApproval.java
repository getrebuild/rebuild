/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.approval.ApprovalHelper;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.approval.ApprovalStepService;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.LazyWaitDetailsFinished;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerException;
import com.rebuild.core.service.trigger.TriggerResult;
import com.rebuild.core.support.CommonsLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.NamedThreadLocal;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rebuild.core.support.CommonsLog.TYPE_TRIGGER;

/**
 * @author devezhao
 * @since 2020/7/31
 */
@Slf4j
public class AutoApproval extends TriggerAction implements LazyWaitDetailsFinished {

    // 延迟执行
    private static final ThreadLocal<List<AutoApproval>> LAZY_AUTOAPPROVAL = new NamedThreadLocal<>("Lazy AutoApproval");
    private OperatingContext keepOperatingContext;

    public AutoApproval(ActionContext context) {
        super(context);
    }

    @Override
    public ActionType getType() {
        return ActionType.AUTOAPPROVAL;
    }

    @Override
    public boolean isUsableSourceEntity(int entityCode) {
        return MetadataHelper.hasApprovalField(MetadataHelper.getEntity(entityCode));
    }

    @Override
    public Object execute(OperatingContext operatingContext) throws TriggerException {
        this.keepOperatingContext = operatingContext;
        List<AutoApproval> lazyed;
        if ((lazyed = isLazy(false)) != null) {
            lazyed.add(this);
            log.info("Lazy AutoApproval : {}", lazyed);
            return FLAG_LAZY;
        }

        final ID recordId = operatingContext.getFixedRecordId();

        ApprovalState currentState = ApprovalHelper.getApprovalState(recordId);
        if (currentState == ApprovalState.PROCESSING || currentState == ApprovalState.APPROVED) {
            return TriggerResult.wran("Ignored state");
        }

        String useApproval = ((JSONObject) actionContext.getActionContent()).getString("useApproval");

        // 优先使用当前用户
        ID approver = ObjectUtils.defaultIfNull(UserContextHolder.getUser(true), UserService.SYSTEM_USER);
        ID approvalId = ID.isId(useApproval) ? ID.valueOf(useApproval) : null;

        // v2.10
        boolean submitMode = ((JSONObject) actionContext.getActionContent()).getBooleanValue("submitMode");

        if (submitMode) {
            Assert.notNull(approvalId, "[useApproval] not be null");
            Application.getBean(ApprovalStepService.class).txAutoSubmit(recordId, approver, approvalId);
        } else {
            Application.getBean(ApprovalStepService.class).txAutoApproved(recordId, approver, approvalId);
        }

        return TriggerResult.success(Collections.singletonList(recordId));
    }

    @Override
    public String toString() {
        String s = super.toString();
        if (keepOperatingContext != null) s += "#OperatingContext:" + keepOperatingContext;
        return s;
    }

    // --

    /**
     */
    public static void setLazy() {
        LAZY_AUTOAPPROVAL.set(new ArrayList<>());
    }

    /**
     * @param once
     * @return
     */
    public static List<AutoApproval> isLazy(boolean once) {
        List<AutoApproval> lazyed = LAZY_AUTOAPPROVAL.get();
        if (lazyed != null && once) LAZY_AUTOAPPROVAL.remove();
        return lazyed;
    }

    /**
     * @return
     */
    public static int executeLazy() {
        List<AutoApproval> lazyed = isLazy(true);
        if (lazyed != null) {
            for (AutoApproval a : lazyed) {
                log.info("Lazy AutoApproval execute : {}", a);
                Object res = a.execute(a.keepOperatingContext);

                CommonsLog.createLog(TYPE_TRIGGER,
                        UserService.SYSTEM_USER, a.getActionContext().getConfigId(), res.toString());
            }
        }
        return lazyed == null ? 0 : lazyed.size();
    }
}
