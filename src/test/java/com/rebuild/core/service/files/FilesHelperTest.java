/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.files;

import cn.devezhao.persist4j.Record;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 2019/11/15
 */
public class FilesHelperTest extends TestSupport {

    @BeforeEach
    public void setUpPerMethod() {
        UserContextHolder.setUser(UserService.ADMIN_USER);
    }

    @Test
    void getFolders() {
        Record folder = EntityHelper.forNew(EntityHelper.AttachmentFolder, SIMPLE_USER);
        folder.setString("name", "123456");
        folder = Application.getService(EntityHelper.AttachmentFolder).create(folder);

        System.out.println(FilesHelper.getAccessableFolders(SIMPLE_USER, null));
        Application.getService(EntityHelper.AttachmentFolder).delete(folder.getPrimary());
    }
}