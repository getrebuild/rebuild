/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.approval.ApprovalStepService;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NamedThreadLocal;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao
 * @since 2020/7/31
 */
@Slf4j
public class AutoApproval extends TriggerAction {

    private static final ThreadLocal<List<AutoApproval>> LAZY_AUTOAPPROVAL = new NamedThreadLocal<>("Lazy AutoApproval");

    private OperatingContext operatingContext;

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
    public void execute(OperatingContext operatingContext) throws TriggerException {
        this.operatingContext = operatingContext;

        List<AutoApproval> lazyed;
        if ((lazyed = isLazyAutoApproval(false)) != null) {
            lazyed.add(this);
            log.info("Lazy AutoApproval : {}", lazyed);
            return;
        }

        ID recordId = operatingContext.getAnyRecord().getPrimary();
        String useApproval = ((JSONObject) actionContext.getActionContent()).getString("useApproval");

//        String useApprover = ((JSONObject) actionContext.getActionContent()).getString("useApprover");
//        ID approver = operatingContext.getOperator();
//        if (ID.isId(useApprover)) approver = ID.valueOf(useApprover);
        ID approver = UserService.SYSTEM_USER;
        ID approvalId = ID.isId(useApproval) ? ID.valueOf(useApproval) : null;

        // v2.10
        boolean submitMode = ((JSONObject) actionContext.getActionContent()).getBooleanValue("submitMode");

        if (submitMode) {
            Assert.notNull(useApproval, "[useApproval] not null");
            Application.getBean(ApprovalStepService.class).txAutoSubmit(recordId, approver, approvalId);
        } else {
            Application.getBean(ApprovalStepService.class).txAutoApproved(recordId, approver, approvalId);
        }
    }

    @Override
    public String toString() {
        String s = super.toString();
        if (operatingContext != null) s += "#OperatingContext:" + operatingContext;
        return s;
    }

    // --

    /**
     * 跳过自动审批
     * @see #isLazyAutoApproval(boolean)
     */
    public static void setLazyAutoApproval() {
        LAZY_AUTOAPPROVAL.set(new ArrayList<>());
    }

    /**
     * @return
     */
    public static List<AutoApproval> isLazyAutoApproval(boolean once) {
        List<AutoApproval> lazyed = LAZY_AUTOAPPROVAL.get();
        if (lazyed != null && once) LAZY_AUTOAPPROVAL.remove();
        return lazyed;
    }

    /**
     * @return
     */
    public static int executeLazyAutoApproval() {
        List<AutoApproval> lazyed = isLazyAutoApproval(true);
        if (lazyed != null) {
            for (AutoApproval a : lazyed) {
                log.info("Lazy AutoApproval execute : {}", a);
                a.execute(a.operatingContext);
            }
        }
        return lazyed == null ? 0 : lazyed.size();
    }
}
