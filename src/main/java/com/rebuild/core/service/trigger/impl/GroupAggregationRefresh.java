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

import java.util.*;

/**
 * 分组聚合目标数据刷新。
 * 场景举例：
 * 1.1 新建产品A + 仓库A分组（组合A+A）
 * 1.2 修改仓库A > B（组合B+A），此时原（组合A+A）纪录不会触发更新
 * 2. 因此需要通过强制更新原纪录刷新原组合（组合A+A）记录
 * 3. NOTE 如果组合值均为空，则无法匹配任何目标记录，此时需要全量刷新（通过任一字段必填解决）
 * 4. NOTE 如果只有一个分组字段则全部刷新（性能差）
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

        // 全部刷新
        if (targetWhere.size() <= 1) {
            targetWhere.clear();
            targetWhere.add("(1=1)");
            log.warn("Force refresh all aggregation target(s)");
        }

        Entity entity = this.parent.targetEntity;
        String sql = String.format("select %s,%s from %s where ( %s )",
                StringUtils.join(targetFields, ","),
                entity.getPrimaryField().getName(),
                entity.getName(),
                StringUtils.join(targetWhere, " or "));
        Object[][] targetArray = Application.createQueryNoFilter(sql).array();
        log.info("May refresh target record(s) : {}", targetArray.length);

        ID triggerUser = UserService.SYSTEM_USER;
        ActionContext parentAc = parent.getActionContext();

        Set<ID> refreshedIds = new HashSet<>();
        refreshedIds.add(parent.targetRecordId);

        for (Object[] o : targetArray) {
            ID targetRecordId = (ID) o[o.length - 1];
            if (refreshedIds.contains(targetRecordId)) continue;
            else refreshedIds.add(targetRecordId);

            List<String> qFieldsFollow = new ArrayList<>();
            List<String> qFieldsFollow2 = new ArrayList<>();
            for (int i = 0; i < o.length - 1; i++) {
                String[] source = fieldsRefresh.get(i);
                if (o[i] == null) {
                    qFieldsFollow.add(String.format("%s is null", source[1]));
                } else {
                    qFieldsFollow.add(String.format("%s = '%s'", source[1], o[i]));
                    qFieldsFollow2.add(String.format("%s = '%s'", source[1], o[i]));
                }
            }

            ID useReferenceId = null;
            // 1.直接获取
            for (int i = 0; i < o.length - 1; i++) {
                Object mayId = o[i];
                if (ID.isId(mayId) && ((ID) mayId).getEntityCode() > 100) {
                    useReferenceId = (ID) mayId;
                    break;
                }
            }
            // 2.强制查找
            if (useReferenceId == null) {
                sql = String.format("select %s from %s where %s",
                        parent.sourceEntity.getPrimaryField().getName(),
                        parent.sourceEntity.getName(),
                        StringUtils.join(qFieldsFollow2, " or "));
                Object[] found = Application.getQueryFactory().unique(sql);
                useReferenceId = found == null ? null : (ID) found[0];
            }

            if (useReferenceId == null) {
                log.warn("No any source-id found, ignored : {}", Arrays.toString(o));
                continue;
            }

            if (refreshedIds.contains(useReferenceId)) continue;
            else refreshedIds.add(useReferenceId);

            ActionContext actionContext = new ActionContext(null,
                    parentAc.getSourceEntity(), parentAc.getActionContent(), parentAc.getConfigId());

            GroupAggregation ga = new GroupAggregation(actionContext);
            ga.sourceEntity = parent.sourceEntity;
            ga.targetEntity = parent.targetEntity;
            ga.targetRecordId = targetRecordId;
            ga.followSourceWhere = StringUtils.join(qFieldsFollow, " and ");

            Record fakeSourceRecord = EntityHelper.forUpdate(useReferenceId, triggerUser, false);
            OperatingContext oCtx = OperatingContext.create(triggerUser, BizzPermission.NONE, fakeSourceRecord, fakeSourceRecord);

            try {
                ga.execute(oCtx);
            } catch (Exception ex) {
                log.error("Error on refresh trigger ({}) record : {}", parentAc.getConfigId(), o[0], ex);
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
