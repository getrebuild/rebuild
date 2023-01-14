/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author RB
 * @since 2022/8/15
 * @see GroupAggregationRefresh
 */
@Slf4j
public class FieldAggregationRefresh {

    final private FieldAggregation parent;
    final private OperatingContext operatingContext;

    protected FieldAggregationRefresh(FieldAggregation parent, OperatingContext operatingContext) {
        this.parent = parent;
        this.operatingContext = operatingContext;
    }

    /**
     */
    public void refresh() {
        if (operatingContext.getBeforeRecord() == null || operatingContext.getAfterRecord() == null) {
            return;
        }

        ID triggerUser = UserService.SYSTEM_USER;
        ActionContext parentAc = parent.getActionContext();

        // FIELD.ENTITY
        String[] targetFieldEntity = ((JSONObject) parentAc.getActionContent()).getString("targetEntity").split("\\.");
        String followSourceField = targetFieldEntity[0];

        final ID beforeValue = operatingContext.getBeforeRecord().getID(followSourceField);
        final ID afterValue = operatingContext.getAfterRecord().getID(followSourceField);

        // 之前未聚合
        if (beforeValue == null) return;
        // 未更新
        if (beforeValue.equals(afterValue)) return;

        ActionContext actionContext = new ActionContext(null,
                parentAc.getSourceEntity(), parentAc.getActionContent(), parentAc.getConfigId());

        FieldAggregation fa = new FieldAggregation(actionContext, true);
        fa.sourceEntity = parent.sourceEntity;
        fa.targetEntity = parent.targetEntity;
        fa.targetRecordId = beforeValue;
        fa.followSourceWhere = String.format("%s = '%s'", followSourceField, beforeValue);

        Record fakeSourceRecord = EntityHelper.forUpdate(beforeValue, triggerUser, false);
        OperatingContext oCtx = OperatingContext.create(triggerUser, BizzPermission.NONE, fakeSourceRecord, fakeSourceRecord);

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
