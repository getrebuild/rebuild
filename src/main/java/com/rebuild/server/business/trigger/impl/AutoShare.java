/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.trigger.ActionContext;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.business.trigger.TriggerException;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.privileges.PrivilegesGuardInterceptor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;

/**
 * @author devezhao
 * @since 2019/8/23
 */
public class AutoShare extends AutoAssign {

    private static final Log LOG = LogFactory.getLog(AutoShare.class);

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
                && !Application.getSecurityManager().allow(operatingContext.getOperator(), recordId, BizzPermission.SHARE)) {
            LOG.warn("No privileges to share record of target: " + recordId);
            return;
        }

        JSONArray shareTo = content.getJSONArray("shareTo");
        Set<ID> toUsers = UserHelper.parseUsers(shareTo, recordId);
        if (toUsers.isEmpty()) {
            return;
        }

        String hasCascades = ((JSONObject) context.getActionContent()).getString("cascades");
        String[] cascades = null;
        if (StringUtils.isNotBlank(hasCascades)) {
            cascades = hasCascades.split("[,]");
        }

        for (ID toUser : toUsers) {
            if (allowNoPermissionShare) {
                PrivilegesGuardInterceptor.setNoPermissionPassOnce(recordId);
            }

            Application.getEntityService(context.getSourceEntity().getEntityCode()).share(recordId, toUser, cascades);
        }
    }
}
