/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.core.support.task.TaskExecutors;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author ZHAO
 * @since 2020/5/12
 */
public class ClassificationFileImporterTest extends TestSupport {

    @Test
    public void exec() throws FileNotFoundException {
        ID newClass = getClassification();
        File file = ResourceUtils.getFile("classpath:classification-demo.xlsx");

        ClassificationFileImporter importer = new ClassificationFileImporter(newClass, file);
        TaskExecutors.run((HeavyTask<?>) importer.setUser(UserService.SYSTEM_USER));
        System.out.println("ClassificationFileImporter : " + importer.getSucceeded());
    }

    private static ID lastAdded = null;

    /**
     * 新建一个分类
     *
     * @return
     */
    static ID getClassification() {
        if (lastAdded == null) {
            Record record = EntityHelper.forNew(EntityHelper.Classification, UserService.ADMIN_USER);
            record.setString("name", "测试" + System.currentTimeMillis());
            record = Application.getCommonsService().create(record);
            lastAdded = record.getPrimary();
        }
        System.out.println("Added Mock Classification : " + lastAdded);
        return lastAdded;
    }
}