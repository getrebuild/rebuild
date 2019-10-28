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

package com.rebuild.server.configuration.portals;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.entity.DisplayType;
import org.junit.Test;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public class MultiSelectManagerTest extends TestSupport {

    @Test
    public void getSelectList() {
        Entity test = getTestEntity();
        JSON ret = MultiSelectManager.instance.getSelectList(test.getField(DisplayType.MULTISELECT.name()));
        System.out.println(ret);
    }

    @Test
    public void getDefaultValue() {
        Entity test = getTestEntity();
        Long ret = MultiSelectManager.instance.getDefaultValue(test.getField(DisplayType.MULTISELECT.name()));
        System.out.println(ret);
    }
}