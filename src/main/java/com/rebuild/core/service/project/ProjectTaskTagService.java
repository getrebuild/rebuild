/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.project;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import org.springframework.stereotype.Service;

/**
 * @author devezhao
 * @since 2020/11/21
 */
@Service
public class ProjectTaskTagService extends BaseConfigurationService {

    protected ProjectTaskTagService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.ProjectTaskTag;
    }

    @Override
    protected void throwIfNotSelf(ID cfgid) {
        // TODO 普通用户可创建 ???
    }

    @Override
    protected void cleanCache(ID tagId) {
        Object[] p = Application.createQueryNoFilter(
                "select projectId from ProjectTaskTag where tagId = ?")
                .setParameter(1, tagId)
                .unique();
        TaskTagManager.instance.clean(p[0]);
    }
}
