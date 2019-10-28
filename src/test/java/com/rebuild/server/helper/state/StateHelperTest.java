/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper.state;

import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.TestSupport;
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