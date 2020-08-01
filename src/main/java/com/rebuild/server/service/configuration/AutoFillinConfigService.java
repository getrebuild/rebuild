/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.configuration;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.AutoFillinManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.privileges.AdminGuard;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/18
 */
public class AutoFillinConfigService extends ConfigurationService implements AdminGuard {

	protected AutoFillinConfigService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	@Override
	public int getEntityCode() {
		return EntityHelper.AutoFillinConfig;
	}

	@Override
	protected void cleanCache(ID configId) {
		Object[] cfg = Application.createQueryNoFilter(
				"select belongEntity,belongField from AutoFillinConfig where configId = ?")
				.setParameter(1, configId)
				.unique();
		if (cfg != null) {
			Field dest = MetadataHelper.getField((String) cfg[0], (String) cfg[1]);
			AutoFillinManager.instance.clean(dest);
		}
	}
}
