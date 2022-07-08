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
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionFactory;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 分组聚合目标数据刷新。
 * 场景举例：
 * 1. 新建产品A + 仓库A分组（组合A+A）
 * 2. 之后修改了仓库A > B（组合A+B），此时原（组合A+A）纪录不会更新
 * 3. 这里需要强制更新相关原纪录
 *
 * @author RB
 * @since 2022/7/8
 */
@Slf4j
public class GroupAggregationRefresh {

    private GroupAggregation parent;
    private List<String[]> fieldsRefresh;

    protected GroupAggregationRefresh(GroupAggregation parent, List<String[]> fieldsRefresh) {
        this.parent = parent;
        this.fieldsRefresh = fieldsRefresh;
    }

    public void refresh() {
        // 目标记录
        List<String> targetWhere = new ArrayList<>();
        // 源记录
        List<String> sourceWhere = new ArrayList<>();

        for (String[] s : fieldsRefresh) {
            targetWhere.add(String.format("%s = '%s'", s[0], s[2]));
            sourceWhere.add(String.format("%s = '%s'", s[1], s[2]));
        }

        Entity entity = this.parent.targetEntity;
        String sql = String.format("select %s from %s where ( %s )",
                entity.getPrimaryField().getName(), entity.getName(),
                StringUtils.join(targetWhere, " or "));
        Object[][] targetArray = Application.createQueryNoFilter(sql).array();

        entity = this.parent.sourceEntity;
        sql = String.format("select %s from %s where ( %s )",
                entity.getPrimaryField().getName(), entity.getName(),
                StringUtils.join(sourceWhere, " or "));
        Object[][] sourceArray = Application.createQueryNoFilter(sql).array();

        log.info("Effect {} targets by {} sources ...", targetArray.length, sourceArray.length);

        ID triggerUser = UserContextHolder.getUser();
        ActionContext parentAc = parent.getActionContext();

        for (Object[] o : sourceArray) {
            ActionContext actionContext = new ActionContext((ID) o[0],
                    parentAc.getSourceEntity(), parentAc.getActionContent(), parentAc.getConfigId());
            TriggerAction triggerAction = ActionFactory.createAction(ActionType.GROUPAGGREGATION.name(), actionContext);

            Record record = EntityHelper.forUpdate((ID) o[0], triggerUser, false);
            OperatingContext oCtx = OperatingContext.create(triggerUser, BizzPermission.NONE, record, record);

            try {
                triggerAction.execute(oCtx);
            } catch (Exception ex) {
                log.error("Error on refresh trigger ({}) record : {}", parentAc.getConfigId(), o[0], ex);
            } finally {
                triggerAction.clean();
            }
        }
    }
}
