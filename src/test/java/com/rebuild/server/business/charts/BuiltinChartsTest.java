/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.charts;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.TestSupport;
import com.rebuild.server.business.charts.builtin.ApprovalList;
import org.junit.Test;

/**
 * @author ZHAO
 * @since 2019/10/14
 */
public class BuiltinChartsTest extends TestSupport {

    @Test
    public void testApprovalList() throws Exception {
        JSON ret = new ApprovalList().setUser(SIMPLE_USER).build();
        System.out.println(ret);
    }
}
