/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.service.base;

import java.util.Set;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.service.BaseService;
import com.rebuild.server.service.notification.Message;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 审批流程
 * 
 * @author devezhao
 * @since 07/11/2019
 */
public class ApprovalStepService extends BaseService {

	protected ApprovalStepService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}

	@Override
	public int getEntityCode() {
		return EntityHelper.RobotApprovalStep;
	}
	
	/**
	 * 审批
	 * 
	 * @param recordOfMain
	 * @param approver
	 * @param cc
	 */
	public void txApprove(Record recordOfMain, Set<ID> nextApprovers, Set<ID> cc) {
		final ID user = Application.getCurrentUser();
		final ID recordId = recordOfMain.getPrimary();
		
		super.createOrUpdate(recordOfMain);
		
		String entityLabel = EasyMeta.getLabel(recordOfMain.getEntity());
		String approveMsg = String.format("有一条%s记录请你审批 @%s", entityLabel, recordId);
		
		// 审批人
		Record step = EntityHelper.forNew(EntityHelper.RobotApprovalStep, user);
		step.setID("recordId", recordId);
		step.setID("approvalId", recordOfMain.getID(EntityHelper.ApprovalId));
		step.setString("node", recordOfMain.getString(EntityHelper.ApprovalStepNode));
		for (ID a : nextApprovers) {
			Record clone = step.clone();
			clone.setID("approver", a);
			super.create(clone);
			Application.getNotifications().send(new Message(user, a, approveMsg, recordId));
		}
		
		// 抄送人
		if (cc != null && !cc.isEmpty()) {
			String ccMsg = String.format("用户 @%s 提交了一条%s审批，请知晓 @%s", user, entityLabel, recordId);
			for (ID c : cc) {
				Application.getNotifications().send(new Message(c, ccMsg, recordId));
			}
		}
	}
}
