/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.task;

import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.general.QuickCodeReindexTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public class QuickCodeReindexTaskTest extends TestSupport {

    @Test
    public void testGenerateQuickCode() {
        Assertions.assertFalse("NHHSJ".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("你 好     hello      世 界")));
        Assertions.assertTrue("HW".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("hello     world     ........")));
        Assertions.assertTrue("HW".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("HelloWorld!")));
        Assertions.assertTrue("NHSJ".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("你好世界")));
        Assertions.assertTrue("NHSJ".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("你 好           世 界")));
    }

    @Test
    public void testGenerateQuickCodeEmpty() {
        // Phone, contains `-`
        Assertions.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("021-123-123")));
        // EMail, contains `@` and `.`
        Assertions.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("1234@getrebuild.com")));
        // URL
        Assertions.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("http://getrebuild.com/aswell")));

        Assertions.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("54325432543")));
        Assertions.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("helloworld")));
        Assertions.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("123456helloworld")));
    }

    @Test
    public void testReindex() {
        new QuickCodeReindexTask(MetadataHelper.getEntity("User")).run();
    }
}
