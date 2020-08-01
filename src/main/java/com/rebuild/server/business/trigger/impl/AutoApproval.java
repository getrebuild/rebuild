/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.trigger.ActionContext;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.business.trigger.TriggerAction;
import com.rebuild.server.business.trigger.TriggerException;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.base.ApprovalStepService;

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
        return MetadataHelper.hasPrivilegesField(MetadataHelper.getEntity(entityCode));
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
