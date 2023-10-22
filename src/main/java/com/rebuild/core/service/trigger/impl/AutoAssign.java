/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.PrivilegesGuardContextHolder;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerException;
import com.rebuild.core.service.trigger.TriggerResult;
import com.rebuild.core.support.KVStorage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * @author devezhao
 * @since 2019/8/23
 */
@Slf4j
public class AutoAssign extends TriggerAction {

    public AutoAssign(ActionContext context) {
        super(context);
    }

    @Override
    public ActionType getType() {
        return ActionType.AUTOASSIGN;
    }

    @Override
    public boolean isUsableSourceEntity(int entityCode) {
        Entity e = MetadataHelper.getEntity(entityCode);
        return MetadataHelper.hasPrivilegesField(e) || e.getMainEntity() != null;
    }

    @Override
    public Object execute(OperatingContext operatingContext) throws TriggerException {
        final JSONObject content = (JSONObject) actionContext.getActionContent();
        final ID recordId = operatingContext.getFixedRecordId();

        JSONArray assignTo = content.getJSONArray("assignTo");
        Set<ID> toUsers = UserHelper.parseUsers(assignTo, recordId, true);
        if (toUsers.isEmpty()) {
            log.warn("No any users found : {}", assignTo);
            return TriggerResult.noMatching();
        }

        ID toUser = null;

        final boolean orderedAssign = ((JSONObject) actionContext.getActionContent()).getIntValue("assignRule") != 2;
        final String orderedAssignKey = "AutoAssignLastAssignTo" + actionContext.getConfigId();
        if (orderedAssign) {
            String lastAssignTo = KVStorage.getCustomValue(orderedAssignKey);
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
            int r = RandomUtils.nextInt(toUsers.size());
            toUser = toUsers.toArray(new ID[0])[r];
        }

        String hasCascades = ((JSONObject) actionContext.getActionContent()).getString("cascades");
        String[] cascades = null;
        if (StringUtils.isNotBlank(hasCascades)) {
            cascades = hasCascades.split(",");
        }

        PrivilegesGuardContextHolder.setSkipGuard(recordId);
        GeneralEntityServiceContextHolder.setFromTrigger(recordId);

        try {
            Application.getEntityService(actionContext.getSourceEntity().getEntityCode())
                    .assign(recordId, toUser, cascades);

            // 当前分配人
            if (orderedAssign) {
                KVStorage.setCustomValue(orderedAssignKey, toUser);
            }

        } finally {
            PrivilegesGuardContextHolder.getSkipGuardOnce();
            GeneralEntityServiceContextHolder.isFromTrigger(true);
        }

        Collection<ID> affected = new ArrayList<>(2);
        affected.add(toUser);
        affected.add(recordId);
        return TriggerResult.success(affected);
    }
}
