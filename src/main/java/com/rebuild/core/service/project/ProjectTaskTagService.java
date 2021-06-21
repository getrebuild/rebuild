/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.project;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.OperationDeniedException;
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
    public int delete(ID tagId) {
        checkManageable(tagId);
        return super.delete(tagId);
    }

    @Override
    public Record update(Record record) {
        checkManageable(record.getPrimary());
        return super.update(record);
    }

    private void checkManageable(ID tagId) {
        final ID user = UserContextHolder.getUser();
        if (!ProjectHelper.isManageable(tagId, user)) throw new OperationDeniedException();
    }

    @Override
    protected void throwIfNotSelf(ID tagId) {
        // 无需检查
    }

    @Override
    protected void cleanCache(ID tagId) {
        Object[] p = Application.getQueryFactory().uniqueNoFilter(tagId, "projectId");
        TaskTagManager.instance.clean(p[0]);
    }
}
