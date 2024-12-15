/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author RB
 * @since 2022/8/15
 * @see GroupAggregationRefresh
 */
@Slf4j
public class FieldAggregationRefresh {

    final private FieldAggregation parent;
    final private OperatingContext operatingContext;
    final private TargetWithMatchFields targetWithMatchFields;

    protected FieldAggregationRefresh(FieldAggregation parent, OperatingContext operatingContext,
                                      TargetWithMatchFields targetWithMatchFields) {
        this.parent = parent;
        this.operatingContext = operatingContext;
        this.targetWithMatchFields = targetWithMatchFields;
    }

    /**
     */
    public void refresh() {
        if (operatingContext.getBeforeRecord() == null || operatingContext.getAfterRecord() == null) return;

        ID triggerUser = UserService.SYSTEM_USER;
        ActionContext parentAc = parent.getActionContext();

        // FIELD.ENTITY
        String[] targetFieldEntity = ((JSONObject) parentAc.getActionContent()).getString("targetEntity").split("\\.");
        String followSourceField = targetFieldEntity[0];

        if (TriggerAction.TARGET_ANY.equals(followSourceField)) {
            refreshWithGroup();
            return;
        }

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

        Record fakeSourceRecord = EntityHelper.forUpdate(operatingContext.getFixedRecordId(), triggerUser, false);
        OperatingContext oCtx = OperatingContext.create(triggerUser, BizzPermission.NONE, fakeSourceRecord, fakeSourceRecord);

        try {
            fa.execute(oCtx);
        } finally {
            fa.clean();
        }
    }

    private void refreshWithGroup() {
        final List<String[]> qFieldsRefresh = targetWithMatchFields.getQFieldsRefresh();

        List<String> targetFields = new ArrayList<>();
        List<String> targetWhere = new ArrayList<>();
        for (String[] s : qFieldsRefresh) {
            targetFields.add(s[0]);
            if (s[2] != null) {
                targetWhere.add(String.format("%s = '%s'", s[0], CommonsUtils.escapeSql(s[2])));
            }
        }

        // 只有1个字段会全量刷新，性能较低
        if (targetWhere.size() <= 1) {
            targetWhere.clear();
            targetWhere.add("(1=1)");
            log.warn("Force refresh all aggregation target(s)");
        }

        // 1.获取待刷新的目标
        final Entity targetEntity = this.parent.targetEntity;
        String sql = String.format("select %s,%s from %s where ( %s )",
                StringUtils.join(targetFields, ","),
                targetEntity.getPrimaryField().getName(),
                targetEntity.getName(),
                StringUtils.join(targetWhere, " or "));
        Object[][] targetRecords4Refresh = Application.createQueryNoFilter(sql).array();
        log.info("Maybe refresh target record(s) : {}", targetRecords4Refresh.length);

        final ID triggerUser = UserService.SYSTEM_USER;
        final ActionContext parentAc = parent.getActionContext();

        // 避免重复的无意义更新
        // NOTE 220905 更新时不能忽略触发源本身的更新
        Set<ID> refreshedIds = new HashSet<>();

        // 2.逐一刷新目标
        for (Object[] o : targetRecords4Refresh) {
            final ID targetRecordId = (ID) o[o.length - 1];
            // 排重
            if (refreshedIds.contains(targetRecordId)) continue;
            else refreshedIds.add(targetRecordId);

            List<String> qFieldsFollow = new ArrayList<>();
            for (int i = 0; i < o.length - 1; i++) {
                String[] s = qFieldsRefresh.get(i);
                if (o[i] == null) {
                    qFieldsFollow.add(String.format("%s is null", s[1]));
                } else {
                    qFieldsFollow.add(String.format("%s = '%s'", s[1], CommonsUtils.escapeSql(o[i])));
                }
            }

            ActionContext actionContext = new ActionContext(null,
                    parentAc.getSourceEntity(), parentAc.getActionContent(), parentAc.getConfigId());

            FieldAggregation fa = new FieldAggregation(actionContext, true);
            fa.sourceEntity = parent.sourceEntity;
            fa.targetEntity = parent.targetEntity;
            fa.targetRecordId = targetRecordId;
            fa.followSourceWhere = StringUtils.join(qFieldsFollow, " and ");

            // FIXME v35 可能导致数据聚合条件中的字段变量不准
            Record fakeSourceRecord = EntityHelper.forUpdate(operatingContext.getFixedRecordId(), triggerUser, false);
            OperatingContext oCtx = OperatingContext.create(triggerUser, BizzPermission.NONE, fakeSourceRecord, fakeSourceRecord);

            try {
                fa.execute(oCtx);
            } finally {
                fa.clean();
            }
        }
    }

    @Override
    public String toString() {
        return parent.toString() + "#Refresh";
    }
}
