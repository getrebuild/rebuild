/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.persist4j.Entity;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;

/**
 * @author devezhao
 * @since 2019/8/17
 */
public class TemplateExtractorTest extends TestSupport {

    @Test
    public void testExtractVars() throws FileNotFoundException {
        File template = ResourceUtils.getFile("classpath:report-template-v2.xlsx");
        Set<String> vars = new TemplateExtractor(template, true).extractVars();
        System.out.println(vars);
        Assertions.assertTrue(vars.size() >= 7);
    }

    @Test
    public void testTransformVars() throws FileNotFoundException {
        File template = ResourceUtils.getFile("classpath:report-template-v2.xlsx");

        Entity test = MetadataHelper.getEntity(TestAllFields);
        System.out.println(new TemplateExtractor(template, true).transformVars(test));

        if (MetadataHelper.containsEntity(SalesOrder)) {
            Entity SalesOrder999 = MetadataHelper.getEntity(SalesOrder);
            System.out.println(new TemplateExtractor(template, true).transformVars(SalesOrder999));
        }
    }

    @Test
    public void testGetRealField() {
        Entity test = MetadataHelper.getEntity(TestAllFields);

        String field = new TemplateExtractor(null, false)
                .transformRealField(test, "所属用户");
        System.out.println(field);

        String field2 = new TemplateExtractor(null, false)
                .transformRealField(test, "所属用户.姓名");
        System.out.println(field2);

        String field3 = new TemplateExtractor(null, false)
                .transformRealField(test, "所属用户.不存在的字段");
        System.out.println(field3);
    }
}
