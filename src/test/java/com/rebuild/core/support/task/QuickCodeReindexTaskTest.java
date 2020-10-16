/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.task;

import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.general.QuickCodeReindexTask;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public class QuickCodeReindexTaskTest extends TestSupport {

    @Test
    public void testGenerateQuickCode() {
        Assert.assertFalse("NHHSJ".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("你 好     hello      世 界")));
        Assert.assertTrue("HW".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("hello     world     ........")));
        Assert.assertTrue("HW".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("HelloWorld!")));
        Assert.assertTrue("NHSJ".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("你好世界")));
        Assert.assertTrue("NHSJ".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("你 好           世 界")));
    }

    @Test
    public void testGenerateQuickCodeEmpty() {
        // Phone, contains `-`
        Assert.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("021-123-123")));
        // EMail, contains `@` and `.`
        Assert.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("1234@getrebuild.com")));
        // URL
        Assert.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("http://getrebuild.com/aswell")));

        Assert.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("54325432543")));
        Assert.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("helloworld")));
        Assert.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("123456helloworld")));
    }

    @Test
    public void testReindex() {
        new QuickCodeReindexTask(MetadataHelper.getEntity("User")).run();
    }
}
