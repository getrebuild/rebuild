/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import com.rebuild.TestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/08
 */
public class VerfiyCodeTest extends TestSupport {

    @Test
    public void testCodeMatching() {
        String key = "testCodeMatching";
        String vcode = VerfiyCode.generate(key);
        boolean isMatch = VerfiyCode.verfiy(key, vcode);
        Assertions.assertTrue(isMatch);
    }
}
