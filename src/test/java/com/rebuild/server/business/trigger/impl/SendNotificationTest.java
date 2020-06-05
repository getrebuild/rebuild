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
import com.rebuild.server.configuration.RobotTriggerManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.configuration.RobotTriggerConfigService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/29
 */
public class SendNotificationTest extends TestSupportWithUser {

    @Test
    public void testExecute() throws Exception {
        // 添加配置
        Application.getSQLExecutor().execute("delete from robot_trigger_config where BELONG_ENTITY = 'TestAllFields'");

        Record triggerConfig = EntityHelper.forNew(EntityHelper.RobotTriggerConfig, UserService.SYSTEM_USER);
        triggerConfig.setString("belongEntity", "TestAllFields");
        triggerConfig.setInt("when", TriggerWhen.CREATE.getMaskValue() + TriggerWhen.DELETE.getMaskValue());
        triggerConfig.setString("actionType", ActionType.SENDNOTIFICATION.name());
        String content = String.format("{ sendTo:['%s'], content:'SENDNOTIFICATION {createdBy} {3782732}' }", SIMPLE_USER);
        triggerConfig.setString("actionContent", content);
        Application.getBean(RobotTriggerConfigService.class).create(triggerConfig);

        Entity test = MetadataHelper.getEntity("TestAllFields");
        RobotTriggerManager.instance.clean(test);

        // 当前未读消息
        int unread = Application.getNotifications().getUnreadMessage(SIMPLE_USER);

        // 保存/删除会发送两条消息
        Application.getSessionStore().set(UserService.ADMIN_USER);
        Record record = EntityHelper.forNew(test.getEntityCode(), UserService.ADMIN_USER);
        record.setString("TestAllFieldsName", "SENDNOTIFICATION");
        record = Application.getEntityService(test.getEntityCode()).create(record);
        Application.getEntityService(test.getEntityCode()).delete(record.getPrimary());

        // 比对消息数
        int unread2 = Application.getNotifications().getUnreadMessage(SIMPLE_USER);
        assertEquals(unread, unread2 - 2);

		// 清理
		Application.getBean(RobotTriggerConfigService.class).delete(triggerConfig.getPrimary());
    }
}
