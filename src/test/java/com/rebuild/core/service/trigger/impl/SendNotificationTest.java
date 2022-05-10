/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.RobotTriggerConfigService;
import com.rebuild.core.service.trigger.TriggerWhen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/29
 */
public class SendNotificationTest extends TestSupport {

    @Test
    public void testExecute() {
        UserContextHolder.setUser(UserService.ADMIN_USER);

        // 添加配置
        Application.getSqlExecutor().execute("delete from robot_trigger_config where BELONG_ENTITY = 'TestAllFields'");

        final ID toUser = SIMPLE_USER;
        final Entity entity = MetadataHelper.getEntity(TestAllFields);

        Record triggerConfig = EntityHelper.forNew(EntityHelper.RobotTriggerConfig, UserService.SYSTEM_USER);
        triggerConfig.setString("belongEntity", TestAllFields);
        triggerConfig.setInt("when", TriggerWhen.CREATE.getMaskValue() + TriggerWhen.DELETE.getMaskValue());
        triggerConfig.setString("actionType", ActionType.SENDNOTIFICATION.name());
        String content = String.format("{ typy:1, sendTo:['%s'], content:'SENDNOTIFICATION {createdBy} {3782732}' }", toUser);
        triggerConfig.setString("actionContent", content);
        Application.getBean(RobotTriggerConfigService.class).create(triggerConfig);

        // 当前未读消息
        int unread = Application.getNotifications().getUnreadMessage(toUser);

        // 保存/删除会发送两条消息
        Record record = EntityHelper.forNew(entity.getEntityCode(), SIMPLE_USER);
        record.setString("TestAllFieldsName", "SENDNOTIFICATION");
        // Create
        record = Application.getEntityService(entity.getEntityCode()).create(record);
        // Delete
        Application.getEntityService(entity.getEntityCode()).delete(record.getPrimary());

        // 比对消息数
        ThreadPool.waitFor(4000);
        int unreadCheck = Application.getNotifications().getUnreadMessage(toUser);
        Assertions.assertEquals(unread, unreadCheck - 2);

        // 清理
        Application.getBean(RobotTriggerConfigService.class).delete(triggerConfig.getPrimary());
    }
}
