/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.configuration;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ProjectManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.privileges.AdminGuard;

/**
 * @author devezhao
 * @since 2020/6/30
 */
public class ProjectPlanConfigService extends ConfigurationService implements AdminGuard {

    protected ProjectPlanConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.ProjectPlanConfig;
    }

    @Override
    protected void cleanCache(ID configId) {
        Object[] p = Application.createQueryNoFilter(
                "select projectId from ProjectPlanConfig where configId = ?")
                .setParameter(1, configId)
                .unique();
        ProjectManager.instance.clean(p == null ? null : p[0]);
    }
}
