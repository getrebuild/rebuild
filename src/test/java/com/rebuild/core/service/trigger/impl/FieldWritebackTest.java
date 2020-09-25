/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.RobotTriggerConfigService;
import com.rebuild.core.service.trigger.TriggerWhen;
import org.junit.Test;

/**
 * @author devezhao
 * @since 2020/2/7
 */
public class FieldWritebackTest extends TestSupport {

    static {
        Application.getSessionStore().set(UserService.ADMIN_USER);
    }

    @Test
    public void testExecute() {
        // 添加配置
        Record triggerConfig = EntityHelper.forNew(EntityHelper.RobotTriggerConfig, UserService.SYSTEM_USER);
        triggerConfig.setString("belongEntity", SalesOrder);
        triggerConfig.setInt("when", TriggerWhen.CREATE.getMaskValue());
        triggerConfig.setString("actionType", ActionType.FIELDWRITEBACK.name());
        String content = "{targetEntity:'relatedAccount.Account999', items:[{sourceField:'createdOn', targetField:'accountName'}]}";
        triggerConfig.setString("actionContent", content);
        Application.getBean(RobotTriggerConfigService.class).create(triggerConfig);

        // 测试执行
        Entity salesOrder999 = MetadataHelper.getEntity(SalesOrder);
        Entity account999 = MetadataHelper.getEntity(Account);

        Record account999Record = EntityHelper.forNew(account999.getEntityCode(), SIMPLE_USER);
        account999Record.setString("accountName", "FWB" + System.nanoTime());
        account999Record = Application.getService(account999.getEntityCode()).create(account999Record);

        Record salesOrder999Record = EntityHelper.forNew(salesOrder999.getEntityCode(), SIMPLE_USER);
        salesOrder999Record.setID("relatedAccount", account999Record.getPrimary());
        Application.getService(account999.getEntityCode()).create(salesOrder999Record);

        // 清理
        Application.getBean(RobotTriggerConfigService.class).delete(triggerConfig.getPrimary());
    }
}