/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.business.trigger.TriggerWhen;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.configuration.RobotTriggerConfigService;
import org.junit.Test;

/**
 * @author devezhao
 * @since 2020/2/7
 */
public class FieldWritebackTest extends TestSupportWithUser {

    @Test
    public void testExecute() throws Exception {
        addExtTestEntities(false);

        // 添加配置
        Record triggerConfig = EntityHelper.forNew(EntityHelper.RobotTriggerConfig, UserService.SYSTEM_USER);
        triggerConfig.setString("belongEntity", "SalesOrder999");
        triggerConfig.setInt("when", TriggerWhen.CREATE.getMaskValue());
        triggerConfig.setString("actionType", ActionType.FIELDWRITEBACK.name());
        String content = "{targetEntity:'relatedAccount.Account999', items:[{sourceField:'createdOn', targetField:'accountName'}]}";
        triggerConfig.setString("actionContent", content);
        Application.getBean(RobotTriggerConfigService.class).create(triggerConfig);

        // 测试执行
        Entity salesOrder999 = MetadataHelper.getEntity("SalesOrder999");
        Entity account999 = MetadataHelper.getEntity("Account999");

        Record account999Record = EntityHelper.forNew(account999.getEntityCode(), getSessionUser());
        account999Record.setString("accountName", "FWB" + System.nanoTime());
        account999Record = Application.getService(account999.getEntityCode()).create(account999Record);

        Record salesOrder999Record = EntityHelper.forNew(salesOrder999.getEntityCode(), getSessionUser());
        salesOrder999Record.setID("relatedAccount", account999Record.getPrimary());
        Application.getService(account999.getEntityCode()).create(salesOrder999Record);

        // 清理
        Application.getBean(RobotTriggerConfigService.class).delete(triggerConfig.getPrimary());
    }
}