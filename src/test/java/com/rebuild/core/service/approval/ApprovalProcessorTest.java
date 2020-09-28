/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.Test;

import java.io.IOException;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/16
 */
public class ApprovalProcessorTest extends TestSupport {

    static {
        UserContextHolder.setUser(UserService.ADMIN_USER);
    }

    @Test
    public void testFlowGroup() throws Exception {
        final ID recordNew = addRecordOfTestAllFields(SIMPLE_USER);
        final ID approvalId = addApprovalConfig();
        ApprovalProcessor processor = new ApprovalProcessor(recordNew, approvalId);

        System.out.println("NextNode : " + processor.getNextNode());
        System.out.println("NextNodes : " + processor.getNextNodes());

        Application.getBean(RobotApprovalConfigService.class).delete(approvalId);
    }

    @Test
    public void testApprove() throws Exception {
        final ID recordNew = addRecordOfTestAllFields(SIMPLE_USER);
        final ID approvalId = addApprovalConfig();
        ApprovalProcessor processor = new ApprovalProcessor(recordNew, approvalId);

        // 提交
        processor.submit(null);

        // 审批
        processor.approve(UserService.ADMIN_USER, ApprovalState.APPROVED, null, null);

        // 当前节点
        System.out.println("CurrentStep : " + processor.getCurrentStep());

        // 已审批节点
        System.out.println("WorkedSteps : " + processor.getWorkedSteps());

        // 撤回
        processor.cancel();

        // Last status
        System.out.println("LastComment : " + ApprovalHelper.getApprovalStatus(recordNew).getLastComment());

        Application.getBean(RobotApprovalConfigService.class).delete(approvalId);
    }

    private ID addApprovalConfig() throws IOException {
        FlowParser flowParser = FlowParserTest.createFlowParser(1);
        Record record = EntityHelper.forNew(EntityHelper.RobotApprovalConfig, UserService.ADMIN_USER);
        record.setString("name", "ApprovalProcessorTest");
        record.setString("belongEntity", TestAllFields);
        record.setString("flowDefinition", flowParser.getFlowDefinition().toJSONString());
        return Application.getBean(RobotApprovalConfigService.class).create(record).getPrimary();
    }
}
