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

package com.rebuild.server.service.configuration;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.business.approval.ApprovalFields2Schema;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.configuration.RobotApprovalManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.bizz.privileges.AdminGuard;

/**
 * 审批流程
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/27
 */
public class RobotApprovalConfigService extends ConfigurationService implements AdminGuard {

	protected RobotApprovalConfigService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	@Override
	public int getEntityCode() {
		return EntityHelper.RobotApprovalConfig;
	}
	
	@Override
	public Record create(Record record) {
		String entity = record.getString("belongEntity");
		new ApprovalFields2Schema(Application.getCurrentUser())
				.createFields(MetadataHelper.getEntity(entity));
		return super.create(record);
	}

	@Override
	public Record update(Record record) {
		if (record.hasValue("flowDefinition")) {
			int inUsed = checkInUsed(record.getPrimary());
			if (inUsed > 0) {
				throw new DataSpecificationException("有 " + inUsed + " 条记录正在使用此流程，禁止修改");
			}
		}
		return super.update(record);
	}

	@Override
	public int delete(ID recordId) {
		int inUsed = checkInUsed(recordId);
		if (inUsed > 0) {
			throw new DataSpecificationException("有 " + inUsed + " 条记录正在使用此流程，禁止删除");
		}
		return super.delete(recordId);
	}
	
	@Override
	protected void cleanCache(ID configId) {
		Object[] cfg = Application.createQueryNoFilter(
				"select belongEntity from RobotApprovalConfig where configId = ?")
				.setParameter(1, configId)
				.unique();
		if (cfg != null) {
			Entity entity = MetadataHelper.getEntity((String) cfg[0]);
			RobotApprovalManager.instance.clean(entity);
		}
	}

	/**
	 * 流程是否正在使用中（处于审核中）
	 *
	 * @param configId
	 * @return
	 */
	public int checkInUsed(ID configId) {
		Object[] belongEntity = Application.createQueryNoFilter(
				"select belongEntity from RobotApprovalConfig where configId = ?")
				.setParameter(1, configId)
				.unique();
		Entity entity = MetadataHelper.getEntity((String) belongEntity[0]);

		String sql = String.format(
				"select count(%s) from %s where approvalId = ? and approvalState = ?",
				entity.getPrimaryField().getName(), entity.getName());
		Object[] inUsed = Application.createQueryNoFilter(sql)
				.setParameter(1, configId)
				.setParameter(2, ApprovalState.PROCESSING.getState())
				.unique();

		return inUsed != null ? ObjectUtils.toInt(inUsed[0]) : 0;
	}
}
