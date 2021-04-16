/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.PrivilegesGuardContextHolder;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.general.EntityService;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Set;

/**
 * @author devezhao
 * @since 2019/8/23
 */
@Slf4j
public class AutoShare extends AutoAssign {

    // 允许无权限共享
    final private boolean allowNoPermissionShare;

    /**
     * @param context
     */
    public AutoShare(ActionContext context) {
        this(context, Boolean.TRUE);
    }

    /**
     * @param context
     * @param allowNoPermissionShare
     */
    public AutoShare(ActionContext context, boolean allowNoPermissionShare) {
        super(context);
        this.allowNoPermissionShare = allowNoPermissionShare;
    }

    @Override
    public ActionType getType() {
        return ActionType.AUTOSHARE;
    }

    @Override
    public void execute(OperatingContext operatingContext) throws TriggerException {
        final JSONObject content = (JSONObject) context.getActionContent();
        final ID recordId = operatingContext.getAnyRecord().getPrimary();

        if (!allowNoPermissionShare
                && !Application.getPrivilegesManager().allow(operatingContext.getOperator(), recordId, BizzPermission.SHARE)) {
            log.warn("No permission to share record of target: " + recordId);
            return;
        }

        JSONArray shareTo = content.getJSONArray("shareTo");
        Set<ID> toUsers = UserHelper.parseUsers(shareTo, recordId, true);
        if (toUsers.isEmpty()) {
            return;
        }

        String hasCascades = ((JSONObject) context.getActionContent()).getString("cascades");
        String[] cascades = null;
        if (StringUtils.isNotBlank(hasCascades)) {
            cascades = hasCascades.split("[,]");
        }

        int shareRights = BizzPermission.READ.getMask();
        if (content.containsKey("withUpdate") && content.getBoolean("withUpdate")) {
            shareRights += BizzPermission.UPDATE.getMask();
        }
        
        final EntityService es = Application.getEntityService(context.getSourceEntity().getEntityCode());
        for (ID toUser : toUsers) {
            if (allowNoPermissionShare) {
                PrivilegesGuardContextHolder.setSkipGuard(recordId);
            }
            es.share(recordId, toUser, cascades, shareRights);
        }
    }
}
