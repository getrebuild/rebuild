/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.configuration;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.configuration.ProjectManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.privileges.AdminGuard;

/**
 * 项目管理
 *
 * @author devezhao
 * @since 2020/6/30
 */
public class ProjectConfigService extends ConfigurationService implements AdminGuard {

    protected ProjectConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.ProjectConfig;
    }

    @Override
    protected void cleanCache(ID configId) {
        ProjectManager.instance.clean(configId);
    }

    /**
     * @param project
     * @param plans
     */
    public void updateProjectAndPlans(Record project, Record[] plans) {
        super.updateRaw(project);
        if (plans != null && plans.length > 0) {
            for (Record plan : plans) {
                super.updateRaw(plan);
            }
        }

        this.cleanCache(null);
        this.cleanCache(project.getPrimary());
    }
}
