/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.configuration;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.RobotTriggerManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.privileges.AdminGuard;

/**
 * 触发器
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/24
 */
public class RobotTriggerConfigService extends ConfigurationService implements AdminGuard {

	protected RobotTriggerConfigService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	@Override
	public int getEntityCode() {
		return EntityHelper.RobotTriggerConfig;
	}
	
	@Override
	protected void cleanCache(ID configId) {
		Object[] cfg = Application.createQueryNoFilter(
				"select belongEntity from RobotTriggerConfig where configId = ?")
				.setParameter(1, configId)
				.unique();
		Entity entity = MetadataHelper.getEntity((String) cfg[0]);
		RobotTriggerManager.instance.clean(entity);
	}
}
