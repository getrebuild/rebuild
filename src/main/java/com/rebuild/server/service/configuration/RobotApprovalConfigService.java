/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
			int inUsed = ntxCheckInUsed(record.getPrimary());
			if (inUsed > 0) {
				throw new DataSpecificationException("有 " + inUsed + " 条记录正在使用此流程，禁止修改");
			}
		}
		return super.update(record);
	}

	@Override
	public int delete(ID recordId) {
		int inUsed = ntxCheckInUsed(recordId);
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
	public int ntxCheckInUsed(ID configId) {
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
