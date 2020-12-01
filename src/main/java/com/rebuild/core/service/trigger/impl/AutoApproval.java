/*
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

/**
 * @author devezhao
 * @since 2020/7/31
 */
public class AutoApproval implements TriggerAction {

    final protected ActionContext context;

    /**
     * @param context
     */
    public AutoApproval(ActionContext context) {
        this.context = context;
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
        ID recordId = operatingContext.getAnyRecord().getPrimary();
        String useApprover = ((JSONObject) context.getActionContent()).getString("useApprover");
        String useApproval = ((JSONObject) context.getActionContent()).getString("useApproval");

        Application.getBean(ApprovalStepService.class).txAutoApproved(
                recordId,
                ID.isId(useApprover) ? ID.valueOf(useApprover) : null,
                ID.isId(useApproval) ? ID.valueOf(useApproval) : null);
    }
}
