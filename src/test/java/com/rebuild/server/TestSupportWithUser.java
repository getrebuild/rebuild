/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
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
        return SIMPLE_USER;
    }

    /**
     * 添加一条测试记录
     *
     * @return
     */
    protected ID addRecordOfTestAllFields() {
        Entity testEntity = MetadataHelper.getEntity(TEST_ENTITY);
        // 自动添加权限
        if (!Application.getSecurityManager().allowedC(getSessionUser(), testEntity.getEntityCode())) {
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
