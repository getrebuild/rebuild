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

import java.io.IOException;

import org.junit.Test;

import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.configuration.RobotApprovalConfigService;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/16
 */
public class ApprovalProcessorTest extends TestSupport {

	@Test
	public void testFlowGroup() throws Exception {
		Application.getSessionStore().set(UserService.ADMIN_USER);
		ID approvalId = null;
		try {
			approvalId = addApprovalConfig();
			ID recordId = addTestRecord();
			ApprovalProcessor processor = new ApprovalProcessor(UserService.ADMIN_USER, recordId, approvalId);
			System.out.println("NextNode : " + processor.getNextNode());
			System.out.println("NextNodes : " + processor.getNextNodes());
		} finally {
			if (approvalId != null) {
				Application.getBean(RobotApprovalConfigService.class).delete(approvalId);
			}
			Application.getSessionStore().clean();
		}
	}
	
	@Test
	public void testApprove() throws Exception {
		Application.getSessionStore().set(UserService.ADMIN_USER);
		ID approvalId = null;
		try {
			approvalId = addApprovalConfig();
			ID recordId = addTestRecord();
			ApprovalProcessor processor = new ApprovalProcessor(UserService.ADMIN_USER, recordId, approvalId);
			
			// 提交
			processor.submit(null);
			
			// 审批
			processor.approve(UserService.ADMIN_USER, ApprovalState.REJECTED, null, null);
			
			// 当前节点
			System.out.println("CurrentStep : " + processor.getCurrentStep());
			
			// 已审批
			System.out.println("WorkedSteps : " + processor.getWorkedSteps());
			
		} finally {
			if (approvalId != null) {
				Application.getBean(RobotApprovalConfigService.class).delete(approvalId);
			}
			Application.getSessionStore().clean();
		}
	}
	
	
	
	/**
	 * @return
	 * @throws IOException
	 */
	static ID addApprovalConfig() throws IOException {
		FlowParser flowParser = FlowParserTest.createFlowParser(0);
		Record record = EntityHelper.forNew(EntityHelper.RobotApprovalConfig, UserService.ADMIN_USER);
		record.setString("name", "ApprovalProcessorTest");
		record.setString("belongEntity", TEST_ENTITY);
		record.setString("flowDefinition", flowParser.getFlowDefinition().toJSONString());
		record = Application.getBean(RobotApprovalConfigService.class).create(record);
		return record.getPrimary();
	}
	
	/**
	 * @return
	 */
	static ID addTestRecord() {
		Entity test = MetadataHelper.getEntity(TEST_ENTITY);
		Record record = EntityHelper.forNew(test.getEntityCode(), UserService.ADMIN_USER);
		record = Application.getGeneralEntityService().create(record);
		return record.getPrimary();
	}
}
