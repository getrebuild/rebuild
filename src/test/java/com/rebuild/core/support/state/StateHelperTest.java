/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.state;

import com.alibaba.fastjson.JSONArray;
import com.rebuild.TestSupport;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/09/06
 */
public class StateHelperTest extends TestSupport {

    @Test
    public void isStateClass() {
        assertTrue(StateHelper.isStateClass(HowtoState.class.getName()));
        assertFalse(StateHelper.isStateClass(AppUtils.class.getName()));
    }

    @Test
    public void valueOf() {
        System.out.println(StateHelper.valueOf(HowtoState.class.getName(), 1));
    }

    @Test
    public void getStateOptions() {
        JSONArray options = StateManager.instance.getStateOptions(HowtoState.class.getName());
        System.out.println(JSONUtils.prettyPrint(options));
    }
}