/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.Entity2Schema;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.core.support.task.TaskExecutors;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/29
 */
public class MetaschemaImporterTest extends TestSupport {

    @Test
    void testImport() throws Exception {
        File file = ResourceUtils.getFile("classpath:metaschema-test.json");
        String text = FileUtils.readFileToString(file, "utf-8");
        JSONObject data = JSON.parseObject(text);
        String entityName = data.getString("entity");

        if (MetadataHelper.containsEntity(entityName)) {
            new Entity2Schema(UserService.ADMIN_USER)
                    .dropEntity(MetadataHelper.getEntity(entityName), true);
        }

        MetaschemaImporter importer = new MetaschemaImporter(data);
        TaskExecutors.run((HeavyTask<?>) importer.setUser(UserService.ADMIN_USER));
    }

    @Test
    void verfiy() throws IOException {
        File file = ResourceUtils.getFile("classpath:metaschema-test.json");
        String text = FileUtils.readFileToString(file, "utf-8");
        JSONObject data = JSON.parseObject(text);

        System.out.println(new MetaschemaImporter(data).verfiy());
    }
}
