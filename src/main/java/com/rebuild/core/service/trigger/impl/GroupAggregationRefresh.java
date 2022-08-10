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
import java.util.List;

/**
 * 分组聚合目标数据刷新。
 * 场景举例：
 * 1.1 新建产品A + 仓库A分组（组合A+A）
 * 1.2 修改仓库A > B（组合A+B），此时原（组合A+A）纪录不会更新
 * 2. 这里需要强制更新相关原纪录
 * 3. NOTE 如果组合值均为空，则无法匹配任何目标记录，此时需要全量刷新（通过任一字段必填解决）
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

    public void refresh() {
        List<String> targetFields = new ArrayList<>();
        List<String> targetWhere = new ArrayList<>();
        for (String[] s : fieldsRefresh) {
            targetFields.add(s[0]);
            if (s[2] != null) {
                targetWhere.add(String.format("%s = '%s'", s[0], s[2]));
            }
        }

        Entity entity = this.parent.targetEntity;
        String sql = String.format("select %s,%s from %s where ( %s )",
                StringUtils.join(targetFields, ","),
                entity.getPrimaryField().getName(),
                entity.getName(),
                StringUtils.join(targetWhere, " or "));
        Object[][] targetArray = Application.createQueryNoFilter(sql).array();
        log.info("Refreshing target record(s) : {}", targetArray.length);

        ID triggerUser = UserService.SYSTEM_USER;
        ActionContext parentAc = parent.getActionContext();

        for (Object[] o : targetArray) {
            ID targetRecordId = (ID) o[o.length - 1];
            if (targetRecordId.equals(parent.targetRecordId)) continue;

            ActionContext actionContext = new ActionContext(null,
                    parentAc.getSourceEntity(), parentAc.getActionContent(), parentAc.getConfigId());

            GroupAggregation ga = new GroupAggregation(actionContext);
            ga.sourceEntity = parent.sourceEntity;
            ga.targetEntity = parent.targetEntity;
            ga.targetRecordId = targetRecordId;

            List<String> qFieldsFollow = new ArrayList<>();
            for (int i = 0; i < o.length - 1; i++) {
                String[] source = fieldsRefresh.get(i);
                if (o[i] == null) {
                    qFieldsFollow.add(String.format("%s is null", source[1]));
                } else {
                    qFieldsFollow.add(String.format("%s = '%s'", source[1], o[i]));
                }
            }
            ga.followSourceWhere = StringUtils.join(qFieldsFollow, " and ");

            Record record = EntityHelper.forUpdate((ID) o[0], triggerUser, false);
            OperatingContext oCtx = OperatingContext.create(triggerUser, BizzPermission.NONE, record, record);

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
