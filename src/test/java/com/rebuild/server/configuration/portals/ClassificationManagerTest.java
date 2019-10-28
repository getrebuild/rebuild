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
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import org.junit.Test;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/06
 */
public class ClassificationManagerTest extends TestSupport {

	@Test
	public void testFindByName() throws Exception {
		Entity test = MetadataHelper.getEntity(TEST_ENTITY);
		Field classification = test.getField("classification");
		
		ID itemId = ClassificationManager.instance.findItemByName("南京", classification);
		if (itemId != null) {
			String fullName = ClassificationManager.instance.getFullName(itemId);
			System.out.println(itemId + " > " + fullName);
		}
		System.out.println(itemId);
		
		itemId = ClassificationManager.instance.findItemByName("江苏.南京", classification);
		System.out.println(itemId);
		
		itemId = ClassificationManager.instance.findItemByName("江苏.无效的", classification);
		System.out.println(itemId);
	}
}
