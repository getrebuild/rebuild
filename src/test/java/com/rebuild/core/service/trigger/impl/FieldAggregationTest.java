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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.*;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/29
 */
public class FieldAggregationTest extends TestSupport {

    @Test
    public void testExecute() {
        UserContextHolder.setUser(UserService.ADMIN_USER);

        // 添加配置
        Record triggerConfig = EntityHelper.forNew(EntityHelper.RobotTriggerConfig, UserService.SYSTEM_USER);
        triggerConfig.setString("belongEntity", SalesOrderItem);
        triggerConfig.setInt("when", TriggerWhen.CREATE.getMaskValue() + TriggerWhen.DELETE.getMaskValue());
        triggerConfig.setString("actionType", ActionType.FIELDAGGREGATION.name());
        String content = "{targetEntity:'SalesOrder999Id.SalesOrder999', items:[{sourceField:'',calcMode:'SUM', targetField:'totalAmount'}]}";
        triggerConfig.setString("actionContent", content);
        Application.getBean(RobotTriggerConfigService.class).create(triggerConfig);

        // 测试执行
        Entity testEntity = MetadataHelper.getEntity(SalesOrderItem);
        RobotTriggerManager.instance.clean(testEntity);

        Record testRecord = EntityHelper.forNew(testEntity.getEntityCode(), UserService.SYSTEM_USER);

        TriggerAction[] as = RobotTriggerManager.instance.getActions(ID.newId(testEntity.getEntityCode()), TriggerWhen.CREATE);
        for (TriggerAction action : as) {
            action.execute(OperatingContext.create(UserService.SYSTEM_USER, BizzPermission.CREATE, null, testRecord));
        }

        // 清理
        Application.getBean(RobotTriggerConfigService.class).delete(triggerConfig.getPrimary());
    }

    @Test
    public void testEvaluator() {
        UserContextHolder.setUser(UserService.ADMIN_USER);

        Entity sourceEntity = MetadataHelper.getEntity(SalesOrder);
        String filterSql = String.format("relatedAccount = '%s'", ID.newId(sourceEntity.getEntityCode()));

        JSONObject configUseFormula = JSON.parseObject(
                "{ targetField:'totalAmount', calcMode:'FORMULA', sourceFormula:'{totalAmount$$$$SUM}*1.35' }");
        // SQL: 1140/42000/this is incompatible with sql_mode=only_full_group_by
        new AggregationEvaluator(configUseFormula, sourceEntity, filterSql)
                .eval();

        JSONObject configUseMAX = JSON.parseObject(
                "{ targetField:'totalAmount', calcMode:'MAX', sourceField:'totalAmount' }");
        new AggregationEvaluator(configUseMAX, sourceEntity, filterSql)
                .eval();
    }
}
