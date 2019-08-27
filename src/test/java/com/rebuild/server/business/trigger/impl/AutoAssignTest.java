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

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.business.trigger.TriggerWhen;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.configuration.RobotTriggerConfigService;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/08/27
 */
public class AutoAssignTest extends TestSupport {

    @Test
    public void execute() throws Exception {
        addExtTestEntities(false);
        Application.getSessionStore().set(UserService.ADMIN_USER);

        // 添加配置
        Application.getSQLExecutor().execute("delete from robot_trigger_config where BELONG_ENTITY = '" + TEST_ENTITY + "'");

        Record autoshareConfig = EntityHelper.forNew(EntityHelper.RobotTriggerConfig, UserService.SYSTEM_USER);
        autoshareConfig.setString("belongEntity", TEST_ENTITY);
        autoshareConfig.setInt("when", TriggerWhen.CREATE.getMaskValue());
        autoshareConfig.setString("actionType", ActionType.AUTOASSIGN.name());
        String content = "{cascades:null, assignRule:1, assignTo:['" + SIMPLE_USER.toLiteral() + "']}";
        autoshareConfig.setString("actionContent", content);
        Application.getBean(RobotTriggerConfigService.class).create(autoshareConfig);

        // 测试执行
        try {
            ID testId = addRecordOfTestAllFields();
            ID owningUser = Application.getRecordOwningCache().getOwningUser(testId);
            Assert.assertTrue(SIMPLE_USER.equals(owningUser));
        } finally {
            Application.getBean(RobotTriggerConfigService.class).delete(autoshareConfig.getPrimary());
            Application.getSessionStore().clean();
        }
    }
}