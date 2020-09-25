/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserService;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/04
 */
public class BulkOperatorTest extends TestSupport {

    @Test
    public void share() {
        // 测试记录
        ID recordNew = addRecordOfTestAllFields(SIMPLE_USER);

        // 共享
        BulkContext contextOfShare = new BulkContext(
                UserService.ADMIN_USER, BizzPermission.SHARE, SIMPLE_USER, null, new ID[]{recordNew});
        Application.getGeneralEntityService().bulk(contextOfShare);

        // 清理
        Application.getGeneralEntityService().delete(recordNew);
    }

    @Test
    public void assign() {
        // 测试记录
        ID recordNew = addRecordOfTestAllFields(SIMPLE_USER);

        // 分派
        BulkContext contextOfAssign = new BulkContext(
                UserService.ADMIN_USER, BizzPermission.ASSIGN, SIMPLE_USER, null, new ID[]{recordNew});
        Application.getGeneralEntityService().bulk(contextOfAssign);

        // 删除
        BulkContext contextOfDelete = new BulkContext(
                UserService.ADMIN_USER, BizzPermission.DELETE, null, null, new ID[]{recordNew});
        Application.getGeneralEntityService().bulk(contextOfDelete);
    }
}
