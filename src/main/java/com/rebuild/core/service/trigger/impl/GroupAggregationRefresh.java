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
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 分组聚合目标数据刷新
 *
 * 场景举例：
 * 1.1 新建记录：产品A + 仓库A分组（组合A+A）
 * 1.2 修改记录：仓库A > B（组合B+A），此时原（组合A+A）纪录不会触发更新
 * 2. 因此需要通过强制更新原纪录刷新原组合（组合A+A）记录
 * 3. NOTE 如果组合值均为空，则无法匹配任何原目标记录，此时需要全量刷新（性能差）（可通过任一字段必填解决）
 * 4. NOTE 如果只有一个分组字段，因为无法获知之前的值，则需要全量刷新（性能差）
 *
 * @author RB
 * @since 2022/7/8
 */
@Slf4j
public class GroupAggregationRefresh {

    final private GroupAggregation parent;
    final private List<String[]> fieldsRefresh;

    // fieldsRefresh = [TargetField, SourceField, Value]
    protected GroupAggregationRefresh(GroupAggregation parent, List<String[]> fieldsRefresh) {
        this.parent = parent;
        this.fieldsRefresh = fieldsRefresh;
    }

    /**
     * TODO 存在性能问题（可能刷新过多）
     */
    public void refresh() {
        List<String> targetFields = new ArrayList<>();
        List<String> targetWhere = new ArrayList<>();
        for (String[] s : fieldsRefresh) {
            targetFields.add(s[0]);
            if (s[2] != null) {
                targetWhere.add(String.format("%s = '%s'", s[0], s[2]));
            }
        }

        // 只有 1 个字段会全量刷新，性能较低
        if (targetWhere.size() <= 1) {
            targetWhere.clear();
            targetWhere.add("(1=1)");
            log.warn("Force refresh all aggregation target(s)");
        }

        Entity targetEntity = this.parent.targetEntity;
        String sql = String.format("select %s,%s from %s where ( %s )",
                StringUtils.join(targetFields, ","),
                targetEntity.getPrimaryField().getName(),
                targetEntity.getName(),
                StringUtils.join(targetWhere, " or "));

        Object[][] targetRecords4Refresh = Application.createQueryNoFilter(sql).array();
        log.info("Maybe refresh target record(s) : {}", targetRecords4Refresh.length);

        ID triggerUser = UserService.SYSTEM_USER;
        ActionContext parentAc = parent.getActionContext();

        // 避免重复的无意义更新
        // NOTE 220905 更新时不能忽略触发源本身的更新
        Set<ID> refreshedIds = new HashSet<>();

        for (Object[] o : targetRecords4Refresh) {
            final ID targetRecordId = (ID) o[o.length - 1];
            // 排重
            if (refreshedIds.contains(targetRecordId)) continue;
            else refreshedIds.add(targetRecordId);

            List<String> qFieldsFollow = new ArrayList<>();
            for (int i = 0; i < o.length - 1; i++) {
                String[] source = fieldsRefresh.get(i);
                if (o[i] == null) {
                    qFieldsFollow.add(String.format("%s is null", source[1]));
                } else {
                    qFieldsFollow.add(String.format("%s = '%s'", source[1], o[i]));
                }
            }

            ActionContext actionContext = new ActionContext(null,
                    parentAc.getSourceEntity(), parentAc.getActionContent(), parentAc.getConfigId());

            GroupAggregation ga = new GroupAggregation(actionContext, true);
            ga.sourceEntity = parent.sourceEntity;
            ga.targetEntity = parent.targetEntity;
            ga.targetRecordId = targetRecordId;
            ga.followSourceWhere = StringUtils.join(qFieldsFollow, " and ");

            // FIXME v35 可能导致数据聚合条件中的字段变量不准
            ID fakeSourceId = this.parent.operatingContext.getFixedRecordId();
            Record fakeSourceRecord = EntityHelper.forUpdate(fakeSourceId, triggerUser, false);
            OperatingContext oCtx = OperatingContext.create(triggerUser, BizzPermission.NONE, fakeSourceRecord, fakeSourceRecord);

            try {
                ga.execute(oCtx);
            } finally {
                ga.clean();
            }
        }
    }

    @Override
    public String toString() {
        return parent.toString() + "#Refresh";
    }
}
