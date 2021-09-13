/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import com.rebuild.TestSupport;
import com.rebuild.core.support.task.TaskExecutors;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;

class UserImporterTest extends TestSupport {

    @Test
    void exec() throws FileNotFoundException {
        File useFile = ResourceUtils.getFile("classpath:users-template.xls");

        UserImporter importer = (UserImporter) new UserImporter(useFile, false).setUser(UserService.ADMIN_USER);
        TaskExecutors.run(importer);

        System.out.println("Imports users : " + importer.getSucceeded());
    }
}