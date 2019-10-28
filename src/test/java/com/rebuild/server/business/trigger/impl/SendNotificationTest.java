/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
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

    @Override
    protected ID getSessionUser() {
        return UserService.ADMIN_USER;
    }

    @Test
    public void testExecute() throws Exception {
        // 添加配置
        Application.getSQLExecutor().execute("delete from robot_trigger_config where BELONG_ENTITY = 'TestAllFields'");

        Record triggerConfig = EntityHelper.forNew(EntityHelper.RobotTriggerConfig, UserService.SYSTEM_USER);
        triggerConfig.setString("belongEntity", "TestAllFields");
        triggerConfig.setInt("when", TriggerWhen.CREATE.getMaskValue() + TriggerWhen.DELETE.getMaskValue());
        triggerConfig.setString("actionType", ActionType.SENDNOTIFICATION.name());
        String content = String.format("{ sendTo:['%s'], content:'SENDNOTIFICATION' }", SIMPLE_USER);
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
