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
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.RobotTriggerConfigService;
import com.rebuild.core.service.trigger.TriggerWhen;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 2020/2/7
 * @see FieldWriteback
 */
public class FieldWritebackTest extends TestSupport {

    @Test
    public void testExecute() {
        UserContextHolder.setUser(UserService.ADMIN_USER);

        // 添加配置
        Record triggerConfig = EntityHelper.forNew(EntityHelper.RobotTriggerConfig, UserService.SYSTEM_USER);
        triggerConfig.setString("belongEntity", Account);
        triggerConfig.setInt("when", TriggerWhen.CREATE.getMaskValue());
        triggerConfig.setString("actionType", ActionType.FIELDWRITEBACK.name());
        // 更新自己，新建时将修改时间改为：createdOn+1天
        String content = "{targetEntity:'$PRIMARY$.Account999', items:[{targetField:'modifiedOn', updateMode:'FORMULA', sourceField:'DATEADD(`{createdOn}`, `1D`)' }]}";
        triggerConfig.setString("actionContent", content);
        Application.getBean(RobotTriggerConfigService.class).create(triggerConfig);

        // 测试执行
        Entity account999 = MetadataHelper.getEntity(Account);

        Record account999Record = EntityHelper.forNew(account999.getEntityCode(), SIMPLE_USER);
        account999Record.setString("accountName", "FWB" + System.nanoTime());
        Application.getEntityService(account999.getEntityCode()).create(account999Record);

        // 清理
        Application.getBean(RobotTriggerConfigService.class).delete(triggerConfig.getPrimary());
    }
}