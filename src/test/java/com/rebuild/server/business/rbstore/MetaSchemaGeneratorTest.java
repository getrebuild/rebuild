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

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/28
 */
public class MetaSchemaGeneratorTest extends TestSupport {
	
	@Test
	public void testGenerate() throws Exception {
		addExtTestEntities(false);
		String maybe = "Account999";
		if (MetadataHelper.containsEntity(maybe)) {
			Entity test = MetadataHelper.getEntity(maybe); 
			MetaSchemaGenerator generator = new MetaSchemaGenerator(test);
			JSON schema = generator.generate();
			System.out.println(JSON.toJSONString(schema, true));
		}
	}
	
	@Test
	public void testGenerateHaveSlave() throws Exception {
		addExtTestEntities(false);
		String maybe = "SalesOrder999";
		if (MetadataHelper.containsEntity(maybe)) {
			Entity test = MetadataHelper.getEntity(maybe); 
			MetaSchemaGenerator generator = new MetaSchemaGenerator(test);
			JSON schema = generator.generate();
			System.out.println(JSON.toJSONString(schema, true));
		}
	}
}
