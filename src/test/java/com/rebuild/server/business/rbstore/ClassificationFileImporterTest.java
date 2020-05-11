/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.rbstore;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.TestSupport;
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.server.service.bizz.UserService;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;

/**
 * @author ZHAO
 * @since 2020/5/12
 */
public class ClassificationFileImporterTest extends TestSupport {

    @Test
    public void exec() throws Exception {
        ID newClass = ClassificationImporterTest.getClassification();
        File file = ResourceUtils.getFile("classpath:classification-demo.xlsx");

        ClassificationFileImporter importer = new ClassificationFileImporter(newClass, file);
        TaskExecutors.run(importer.setUser(UserService.SYSTEM_USER));
        System.out.println("ClassificationFileImporter : " + importer.getSucceeded());
    }
}