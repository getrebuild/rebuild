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

package com.rebuild.server.business.approval;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.configuration.RobotApprovalConfigService;
import org.junit.Test;

import java.io.IOException;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/16
 */
public class ApprovalProcessorTest extends TestSupportWithUser {

    @Override
    protected ID getSessionUser() {
        return UserService.ADMIN_USER;
    }

    @Test
    public void testFlowGroup() throws Exception {
		final ID recordNew = addRecordOfTestAllFields();
		final ID approvalId = addApprovalConfig();
        ApprovalProcessor processor = new ApprovalProcessor(UserService.ADMIN_USER, recordNew, approvalId);

        System.out.println("NextNode : " + processor.getNextNode());
        System.out.println("NextNodes : " + processor.getNextNodes());

        Application.getBean(RobotApprovalConfigService.class).delete(approvalId);
    }

    @Test
    public void testApprove() throws Exception {
        final ID recordNew = addRecordOfTestAllFields();
		final ID approvalId = addApprovalConfig();
        ApprovalProcessor processor = new ApprovalProcessor(UserService.ADMIN_USER, recordNew, approvalId);

        // 提交
        processor.submit(null);

        // 审批
        processor.approve(UserService.ADMIN_USER, ApprovalState.REJECTED, null, null);

        // 当前节点
        System.out.println("CurrentStep : " + processor.getCurrentStep());

        // 已审批
        System.out.println("WorkedSteps : " + processor.getWorkedSteps());

        Application.getBean(RobotApprovalConfigService.class).delete(approvalId);
    }

    private ID addApprovalConfig() throws IOException {
        FlowParser flowParser = FlowParserTest.createFlowParser(0);
        Record record = EntityHelper.forNew(EntityHelper.RobotApprovalConfig, UserService.ADMIN_USER);
        record.setString("name", "ApprovalProcessorTest");
        record.setString("belongEntity", TEST_ENTITY);
        record.setString("flowDefinition", flowParser.getFlowDefinition().toJSONString());
        record = Application.getBean(RobotApprovalConfigService.class).create(record);
        return record.getPrimary();
    }
}
