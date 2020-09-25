/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import com.rebuild.TestSupport;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/08
 */
public class RebuildConfigurationTest extends TestSupport {

    @Test
    public void testKnown() {
        for (ConfigurationItem item : ConfigurationItem.values()) {
            String v = RebuildConfiguration.get(item);
            System.out.println(item + " = " + v);
        }
    }

    @Test(expected = SecurityException.class)
    public void getFileOfData() {
        System.out.println(RebuildConfiguration.getFileOfTemp(null));
        // Attack
        System.out.println(RebuildConfiguration.getFileOfTemp("../123.jpg"));
    }
}
