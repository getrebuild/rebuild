/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.configuration;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.configuration.ProjectConfigService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author devezhao
 * @since 2020/7/28
 */
public class ProjectManagerTest extends TestSupportWithUser {

    private static final String USE_CODE = "RBTEST";

    private static ID _LastSavedProject;

    @BeforeClass
    public static void createNewProject() {
        final ProjectConfigService pcs = (ProjectConfigService) Application.getService(EntityHelper.ProjectConfig);

        Object[] exists = Application.createQueryNoFilter(
                "select configId from ProjectConfig where projectCode = ?")
                .setParameter(1, USE_CODE)
                .unique();
        if (exists != null) {
            pcs.delete((ID) exists[0]);
        }

        Record pc = EntityHelper.forNew(EntityHelper.ProjectConfig, UserService.ADMIN_USER);
        pc.setString("projectName", "TEST PROJECT");
        pc.setString("projectCode", USE_CODE);
        pc.setString("members", SIMPLE_USER + "," + UserService.ADMIN_USER);

        Application.getSessionStore().set(UserService.ADMIN_USER);
        try {
            pc = pcs.createProject(pc, 1);
            _LastSavedProject = pc.getPrimary();
        } finally {
            Application.getSessionStore().clean();
        }
    }

    @Test
    public void testGetProject() {
        ConfigEntry[] uses = ProjectManager.instance.getAvailable(SIMPLE_USER);
        Assert.assertTrue(uses.length > 0);

        ProjectManager.instance.getProject(uses[0].getID("id"), null);
    }

    @Test
    public void testGetPlansOfProject() {
        ConfigEntry[] plans = ProjectManager.instance.getPlansOfProject(_LastSavedProject);
        Assert.assertTrue(plans.length > 0);
    }
}