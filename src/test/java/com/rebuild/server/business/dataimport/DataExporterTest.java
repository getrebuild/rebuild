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

package com.rebuild.server.business.dataimport;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 *
 * @author devezhao
 * @since 11/19/2019
 */
public class DataExporterTest extends TestSupport {

    @Test
    public void export() {
        JSONObject query = new JSONObject();
        query.put("entity", TEST_ENTITY);
        List<String> fields = new ArrayList<>();
        for (Field field : MetadataHelper.getEntity(TEST_ENTITY).getFields()) {
            fields.add(field.getName());
        }
        query.put("fields", fields);

        File file = new DataExporter(query).setUser(UserService.ADMIN_USER).export();
        System.out.println(file);
    }
}