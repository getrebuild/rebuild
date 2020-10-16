/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.RobotTriggerConfigService;
import com.rebuild.core.service.trigger.TriggerWhen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/08/27
 */
public class AutoAssignTest extends TestSupport {

    @Test
    public void execute() {
        UserContextHolder.setUser(UserService.ADMIN_USER);

        // 添加配置
        Application.getSqlExecutor().execute("delete from robot_trigger_config where BELONG_ENTITY = '" + TestAllFields + "'");

        Record triggerConfig = EntityHelper.forNew(EntityHelper.RobotTriggerConfig, UserService.SYSTEM_USER);
        triggerConfig.setString("belongEntity", TestAllFields);
        triggerConfig.setInt("when", TriggerWhen.CREATE.getMaskValue());
        triggerConfig.setString("actionType", ActionType.AUTOASSIGN.name());
        String content = "{cascades:null, assignRule:1, assignTo:['" + SIMPLE_USER.toLiteral() + "']}";
        triggerConfig.setString("actionContent", content);
        Application.getBean(RobotTriggerConfigService.class).create(triggerConfig);

        // 测试执行
        ID testId = addRecordOfTestAllFields(SIMPLE_USER);
        ID owningUser = Application.getRecordOwningCache().getOwningUser(testId);
        Assertions.assertEquals(SIMPLE_USER, owningUser);

        // 清理
        Application.getBean(RobotTriggerConfigService.class).delete(triggerConfig.getPrimary());
    }
}