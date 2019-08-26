/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.trigger.ActionContext;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.business.trigger.TriggerAction;
import com.rebuild.server.business.trigger.TriggerException;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.privileges.PrivilegesGuardInterceptor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;

/**
 * @author devezhao
 * @since 2019/8/23
 */
public class AutoAssign implements TriggerAction {

    private static final Log LOG = LogFactory.getLog(AutoAssign.class);

    final protected ActionContext context;

    // 允许无权限分派
    final private boolean allowNoPermissionAssign = true;

    public AutoAssign(ActionContext context) {
        this.context = context;
    }

    @Override
    public ActionType getType() {
        return ActionType.AUTOASSIGN;
    }

    @Override
    public boolean isUsableSourceEntity(int entityCode) {
        Entity entity = MetadataHelper.getEntity(entityCode);
        // 明细不可用
        return entity.getMasterEntity() == null;
    }

    @Override
    public void execute(OperatingContext operatingContext) throws TriggerException {
        final JSONObject content = (JSONObject) context.getActionContent();
        final ID recordId = operatingContext.getAnyRecord().getPrimary();

        if (!allowNoPermissionAssign) {
            if (!Application.getSecurityManager().allowed(
                    operatingContext.getOperator(), recordId, BizzPermission.ASSIGN)) {
                LOG.warn("No privileges to assign record of target: " + recordId);
                return;
            }
        }

        JSONArray assignTo = content.getJSONArray("assignTo");
        Set<ID> toUsers = UserHelper.parseUsers(assignTo, recordId);
        if (toUsers.isEmpty()) {
            return;
        }

        ID toUser = null;

        final boolean orderedAssign = ((JSONObject) context.getActionContent()).getIntValue("assignRule") == 1;
        final String orderedAssignKey = "AutoAssignLastAssignTo" + context.getConfigId();
        if (orderedAssign) {
            String lastAssignTo = SysConfiguration.getCustomValue(orderedAssignKey);
            ID lastAssignToUser = ID.isId(lastAssignTo) ? ID.valueOf(lastAssignTo) : null;

            ID firstUser = null;
            boolean isNext = false;
            for (ID u : toUsers) {
                if (firstUser == null) {
                    firstUser = u;
                }

                if (lastAssignToUser == null || isNext) {
                    toUser = u;
                    break;
                } else if (lastAssignToUser.equals(u)) {
                    isNext = true;
                }
            }

            if (toUser == null) {
                toUser = firstUser;
            }

        } else {
            int rnd = RandomUtils.nextInt(toUsers.size());
            for (ID u : toUsers) {
                if (--rnd == 0) {
                    toUser = u;
                    break;
                }
            }
        }

        String hasCascades = ((JSONObject) context.getActionContent()).getString("cascades");
        String[] cascades = null;
        if (StringUtils.isNotBlank(hasCascades)) {
            cascades = hasCascades.split("[,]");
        }

        if (allowNoPermissionAssign) {
            PrivilegesGuardInterceptor.setNoPermissionPassOnce(recordId);
        }
        Application.getEntityService(context.getSourceEntity().getEntityCode()).assign(recordId, toUser, cascades);

        // 当前分派人
        if (orderedAssign) {
            SysConfiguration.setCustomValue(orderedAssignKey, toUser.toLiteral());
        }
    }

    @Override
    public void prepare(OperatingContext operatingContext) throws TriggerException {
        // Nothings
    }
}
