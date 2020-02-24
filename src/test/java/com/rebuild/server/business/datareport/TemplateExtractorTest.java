/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.datareport;

import cn.devezhao.persist4j.Entity;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.Set;

/**
 * @author devezhao
 * @since 2019/8/17
 */
public class TemplateExtractorTest extends TestSupport {

    @Test
    public void testExtractVars() throws Exception {
        File template = ResourceUtils.getFile("classpath:report-template-v2.xlsx");
        Set<String> vars = new TemplateExtractor(template, true).extractVars(true);
        System.out.println(vars);
        Assert.assertTrue(vars.size() >= 7);
    }

    @Test
    public void testTransformVars() throws Exception {
        File template = ResourceUtils.getFile("classpath:report-template-v2.xlsx");

        Entity test = MetadataHelper.getEntity(TEST_ENTITY);
        System.out.println(new TemplateExtractor(template, true).transformVars(test));

        if (MetadataHelper.containsEntity("SalesOrder999")) {
            Entity SalesOrder999 = MetadataHelper.getEntity("SalesOrder999");
            System.out.println(new TemplateExtractor(template, true).transformVars(SalesOrder999));
        }
    }

    @Test
    public void testGetRealField() throws Exception {
        Entity test = MetadataHelper.getEntity(TEST_ENTITY);

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
