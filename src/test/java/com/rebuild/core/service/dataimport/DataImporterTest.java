/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

import cn.devezhao.bizz.privileges.PrivilegesException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 01/10/2019
 */
public class DataImporterTest extends TestSupport {

    @Test
    public void testParseRule() {
        JSONObject rule = JSON.parseObject("{ file:'dataimports-test.csv', entity:'TestAllFields', repeat_opt:3, fields_mapping:{TestAllFieldsName:5} }");
        ImportRule importsEnter = ImportRule.parse(rule);
        System.out.println("ImportsEnter 1 : " + importsEnter);

        rule = JSON.parseObject("{ file:'dataimports-test.xls', entity:'TestAllFields', repeat_opt:1, repeat_fields:['TestAllFieldsName'], fields_mapping:{TestAllFieldsName:5} }");
        importsEnter = ImportRule.parse(rule);
        System.out.println("ImportsEnter 2 : " + importsEnter);

        rule = JSON.parseObject("{ file:'dataimports-test.xlsx', entity:'TestAllFields', repeat_opt:1, repeat_fields:['TestAllFieldsName'], fields_mapping:{TestAllFieldsName:5} }");
        importsEnter = ImportRule.parse(rule);
        System.out.println("ImportsEnter 3 : " + importsEnter);
    }


    @Test
    public void testErrorRule() {
        JSONObject rule = JSON.parseObject("{ file:'dataimports-test.csv', entity:'TestAllFieldsName', repeat_opt:3, fields_mapping:{ TestAllFieldsName:5 } }");
        rule.remove("entity");

        // No `entity`
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ImportRule.parse(rule));
    }

    @Test
    public void testImports() {
        JSONObject rule = JSON.parseObject("{ file:'dataimports-test.xlsx', entity:'TestAllFields', repeat_opt:2, repeat_fields:['TestAllFieldsName'], owning_user:'001-0000000000000001', fields_mapping:{TestAllFieldsName:5} }");
        ImportRule importsEnter = ImportRule.parse(rule);

        DataImporter dataImports = new DataImporter(importsEnter);
        dataImports.setUser(UserService.ADMIN_USER);
        dataImports.run();
    }
}
