/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author ZHAO
 * @since 2020/4/28
 */
public class ChartsHelperTest {

    @Test
    public void isZero() {
        assertTrue(ChartsHelper.isZero(null));
        assertFalse(ChartsHelper.isZero("123"));
        assertTrue(ChartsHelper.isZero(0));
        assertTrue(ChartsHelper.isZero(0d));
    }
}