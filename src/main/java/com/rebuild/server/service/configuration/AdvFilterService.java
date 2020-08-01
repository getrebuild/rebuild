/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.configuration;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.configuration.portals.AdvFilterManager;
import com.rebuild.server.metadata.EntityHelper;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/30
 */
public class AdvFilterService extends ConfigurationService {
	
	protected AdvFilterService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	@Override
	public int getEntityCode() {
		return EntityHelper.FilterConfig;
	}
	
	@Override
	protected void cleanCache(ID configId) {
		AdvFilterManager.instance.clean(configId);
	}
}
