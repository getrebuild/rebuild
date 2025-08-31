/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.general.EntityService;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerException;
import com.rebuild.core.service.trigger.TriggerResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * @author devezhao
 * @since 2019/8/23
 */
@Slf4j
public class AutoShare extends AutoHoldTriggerAction {

    public AutoShare(ActionContext context) {
        super(context);
    }

    @Override
    public ActionType getType() {
        return ActionType.AUTOSHARE;
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

        JSONArray shareTo = content.getJSONArray("shareTo");
        Set<ID> toUsers = UserHelper.parseUsers(shareTo, recordId, true);
        if (toUsers.isEmpty()) {
            log.warn("No any users found : {}", shareTo);
            return TriggerResult.noMatching();
        }

        Set<ID> relateds42 = null;
        String hasCascades = ((JSONObject) actionContext.getActionContent()).getString("cascades");
        String[] cascades = null;
        if (StringUtils.isNotBlank(hasCascades)) {
            cascades = hasCascades.split(",");
        } else {
            relateds42 = getWillRecords(operatingContext);
        }

        int shareRights = BizzPermission.READ.getMask();
        if (content.getBooleanValue("withUpdate")) {
            shareRights += BizzPermission.UPDATE.getMask();
        }

        ID[] toUsers2 = toUsers.toArray(new ID[0]);
        share(recordId, toUsers2, cascades, shareRights);

        if (!CollectionUtils.isEmpty(relateds42)) {
            for (ID id : relateds42) share(id, toUsers2, null, shareRights);
        }

        Collection<ID> affected = new ArrayList<>(toUsers);
        affected.add(recordId);
        return TriggerResult.success(affected);
    }

    private void share(ID recordId, ID[] toUsers, String[] cascades, int shareRights) {
        EntityService es = Application.getEntityService(recordId.getEntityCode());
        for (ID toUser : toUsers) {
            GeneralEntityServiceContextHolder.setSkipGuard(recordId);
            GeneralEntityServiceContextHolder.setFromTrigger(recordId);

            try {
                es.share(recordId, toUser, cascades, shareRights);
            } finally {
                GeneralEntityServiceContextHolder.isSkipGuardOnce();
                GeneralEntityServiceContextHolder.isAllowForceUpdateOnce();
            }
        }
    }
}
