/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.configuration;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.RebuildApiManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.privileges.AdminGuard;

/**
 * API 鉴权
 *
 * @author devezhao
 * @since 2019/7/23
 */
public class RebuildApiService extends ConfigurationService implements AdminGuard {

    protected RebuildApiService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.RebuildApi;
    }

    @Override
    protected void cleanCache(ID configId) {
        Object[] cfg = Application.createQueryNoFilter(
                "select appId from RebuildApi where uniqueId = ?")
                .setParameter(1, configId)
                .unique();
        RebuildApiManager.instance.clean((String) cfg[0]);
    }
}
