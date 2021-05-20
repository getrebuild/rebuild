/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.project;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.general.ObservableService;
import com.rebuild.core.support.i18n.Language;
import org.springframework.util.Assert;

import java.util.Set;

/**
 * @author devezhao
 * @since 2020/7/27
 */
public abstract class BaseTaskService extends ObservableService {

    protected BaseTaskService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    /**
     * 是否成员，非成员禁止操作
     *
     * @param user
     * @param taskOrProject
     * @return
     */
    protected boolean checkInMembers(ID user, ID taskOrProject) {
        if (user == null) user = UserContextHolder.getUser();
        Assert.notNull(taskOrProject, "taskOrProject");

        ConfigBean c = taskOrProject.getEntityCode() == EntityHelper.ProjectTask
                ? ProjectManager.instance.getProjectByTask(taskOrProject, null)
                : ProjectManager.instance.getProject(taskOrProject, null);
        if (c != null && c.get("members", Set.class).contains(user)) return true;

        throw new DataSpecificationException(Language.L("非项目成员禁止操作"));
    }
}
