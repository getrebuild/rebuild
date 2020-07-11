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
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.bizz.privileges.AdminGuard;

/**
 * 项目/任务面板
 *
 * @author devezhao
 * @since 2020/6/30
 */
public class ProjectPlanConfigService extends ConfigurationService implements AdminGuard {

    /**
     * 流程状态-开始状态: 可新建
     */
    public static final int FLOW_STATUS_START = 1;
    /**
     * 流程状态-进行中: 不可新建、不可完成
     */
    public static final int FLOW_STATUS_PROCESSING = 2;
    /**
     * 流程状态-结束状态: 自动完成
     */
    public static final int FLOW_STATUS_END = 3;

    protected ProjectPlanConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.ProjectPlanConfig;
    }

    @Override
    public int delete(ID planId) {
        Object[] count = Application.createQuery(
                "select count(taskId) from ProjectTask where projectPlanId = ?")
                .setParameter(1, planId)
                .unique();
        if ((Long) count[0] > 0) {
            throw new DataSpecificationException("面板下有 " + count[0] + " 个任务，无法删除");
        }
        return super.delete(planId);
    }

    @Override
    protected void cleanCache(ID configId) {
        Object[] projectId = Application.createQueryNoFilter(
                "select projectId from ProjectPlanConfig where configId = ?")
                .setParameter(1, configId)
                .unique();
        ProjectManager.instance.clean(projectId == null ? null : projectId[0]);
    }
}
