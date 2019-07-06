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

import com.rebuild.server.Application;
import com.rebuild.server.business.approval.ApprovalFields2Schema;
import com.rebuild.server.configuration.RobotApprovalManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.privileges.AdminGuard;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

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
}
