/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.Before;

/**
 * 测试基类
 *
 * @author devezhao
 * @since 2019/10/28
 */
public abstract class TestSupportWithUser extends TestSupport {

    @Before
    public void setUp() {
        if (getSessionUser() != null) {
            Application.getSessionStore().set(getSessionUser());
        }
    }

    @After
    public void setDown() {
        if (getSessionUser() != null) {
            Application.getSessionStore().clean();
        }
    }

    /**
     * 会话/线程用户
     *
     * @return
     */
    protected ID getSessionUser() {
        return UserService.ADMIN_USER;
    }

    /**
     * 添加一条测试记录
     *
     * @return
     */
    protected ID addRecordOfTestAllFields() {
        Entity testEntity = MetadataHelper.getEntity(TEST_ENTITY);
        // 自动添加权限
        if (!Application.getPrivilegesManager().allowCreate(getSessionUser(), testEntity.getEntityCode())) {
            Record p = EntityHelper.forNew(EntityHelper.RolePrivileges, UserService.SYSTEM_USER);
            p.setID("roleId", SIMPLE_ROLE);
            p.setInt("entity", testEntity.getEntityCode());
            p.setString("definition", "{'A':1,'R':1,'C':4,'S':1,'D':1,'U':1}");
            Application.getCommonService().create(p, Boolean.FALSE);
            Application.getUserStore().refreshRole(SIMPLE_ROLE);
        }

        Record record = EntityHelper.forNew(testEntity.getEntityCode(), getSessionUser());
        record.setString("text", "TEXT-" + RandomUtils.nextLong());
        return Application.getGeneralEntityService().create(record).getPrimary();
    }
}
