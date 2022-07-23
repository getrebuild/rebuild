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
import com.rebuild.core.service.approval.ApprovalStepService;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NamedThreadLocal;

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
        String useApprover = ((JSONObject) actionContext.getActionContent()).getString("useApprover");
        String useApproval = ((JSONObject) actionContext.getActionContent()).getString("useApproval");

        Application.getBean(ApprovalStepService.class).txAutoApproved(
                recordId,
                ID.isId(useApprover) ? ID.valueOf(useApprover) : null,
                ID.isId(useApproval) ? ID.valueOf(useApproval) : null);
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
