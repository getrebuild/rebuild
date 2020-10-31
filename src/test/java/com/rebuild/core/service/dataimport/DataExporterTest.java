/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao
 * @since 11/19/2019
 */
public class DataExporterTest extends TestSupport {

    @Test
    public void export() {
        JSONObject query = new JSONObject();
        query.put("entity", TestAllFields);
        List<String> fields = new ArrayList<>();
        for (Field field : MetadataHelper.getEntity(TestAllFields).getFields()) {
            fields.add(field.getName());
        }
        query.put("fields", fields);

        File file = ((DataExporter) new DataExporter(query).setUser(UserService.ADMIN_USER)).export();
        System.out.println(file);
    }
}