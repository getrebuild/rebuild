/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;

/**
 * @author RB
 * @since 2023/4/16
 */
@Slf4j
public class FieldWritebackRefresh {

    final private FieldWriteback parent;
    // ID or ID[]
    final private Object beforeValue;

    protected FieldWritebackRefresh(FieldWriteback parent, Object beforeValue) {
        this.parent = parent;
        this.beforeValue = beforeValue;
    }

    /**
     */
    public void refresh() {
        if (beforeValue instanceof ID[] && ((ID[]) beforeValue).length == 0) return;
        if (NullValue.isNull(beforeValue)) return;

        ID triggerUser = UserService.SYSTEM_USER;
        ActionContext parentAc = parent.getActionContext();

        ActionContext actionContext = new ActionContext(null,
                parentAc.getSourceEntity(), parentAc.getActionContent(), parentAc.getConfigId());

        FieldWriteback fa = new FieldWriteback(actionContext);
        fa.sourceEntity = parent.sourceEntity;
        fa.targetEntity = parent.targetEntity;

        fa.targetRecordIds = new HashSet<>();
        if (beforeValue instanceof ID[]) CollectionUtils.addAll(fa.targetRecordIds, (ID[]) beforeValue);
        else fa.targetRecordIds.add((ID) beforeValue);

        ID fakeSourceId = EntityHelper.newUnsavedId(fa.sourceEntity.getEntityCode());
        Record fakeSourceRecord = EntityHelper.forUpdate(fakeSourceId, triggerUser, false);
        OperatingContext oCtx = OperatingContext.create(triggerUser, BizzPermission.NONE, fakeSourceRecord, fakeSourceRecord);
        fa.targetRecordData = fa.buildTargetRecordData(oCtx, true);

        try {
            fa.execute(oCtx);
        } finally {
            fa.clean();
        }
    }

    @Override
    public String toString() {
        return parent.toString() + "#Refresh";
    }
}
