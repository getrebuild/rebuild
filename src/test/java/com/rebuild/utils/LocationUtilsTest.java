/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.utils;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.TestSupport;
import org.junit.Test;

/**
 * @author devezhao
 * @since 01/31/2019
 */
public class LocationUtilsTest extends TestSupport {

    @Test
    public void getLocation() {
        JSON r = LocationUtils.getLocation("180.162.13.205", false);
        System.out.println(r);

        r = LocationUtils.getLocation("192.168.0.110", false);
        System.out.println(r);

        r = LocationUtils.getLocation("127.0.0.1", false);
        System.out.println(r);
    }
}