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
