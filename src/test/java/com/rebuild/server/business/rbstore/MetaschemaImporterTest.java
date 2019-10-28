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

package com.rebuild.server.business.rbstore;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.Entity2Schema;
import com.rebuild.server.service.bizz.UserService;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/29
 */
public class MetaschemaImporterTest extends TestSupport {
	
	@Before
	public void setUp() {
		Application.getSessionStore().set(UserService.ADMIN_USER);
	}

	@Test
	public void testImport() throws Exception {
		URL dataUrl = MetaschemaImporterTest.class.getClassLoader().getResource("metaschema-test.json");
		String text = FileUtils.readFileToString(new File(dataUrl.toURI()), "utf-8");
		JSONObject data = JSON.parseObject(text);
		String entityName = data.getString("entity");
		
		if (MetadataHelper.containsEntity(entityName)) {
			new Entity2Schema(UserService.ADMIN_USER)
					.dropEntity(MetadataHelper.getEntity(entityName), true);
		}
		
		MetaschemaImporter importer = new MetaschemaImporter(UserService.ADMIN_USER, data);
		Object name = importer.exec();
		System.out.println("IMPORTED ... " + name);
	}
}
