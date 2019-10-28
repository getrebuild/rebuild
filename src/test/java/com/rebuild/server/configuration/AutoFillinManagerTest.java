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

package com.rebuild.server.configuration;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/22
 */
public class AutoFillinManagerTest extends TestSupport {

	@Test
	public void testConversionCompatibleValue() throws Exception {
		Entity test = MetadataHelper.getEntity(TEST_ENTITY);
		Field textField = test.getField("text");
		
		System.out.println(
				AutoFillinManager.instance.conversionCompatibleValue(test.getField("reference"), textField, ID.newId(test.getEntityCode())));
		System.out.println(
				AutoFillinManager.instance.conversionCompatibleValue(test.getField("classification"), textField, ID.newId(EntityHelper.ClassificationData)));
		System.out.println(
				AutoFillinManager.instance.conversionCompatibleValue(test.getField("picklist"), textField, ID.newId(EntityHelper.PickList)));
		System.out.println(
				AutoFillinManager.instance.conversionCompatibleValue(test.getField("datetime"), textField, CalendarUtils.now()));
		System.out.println(
				AutoFillinManager.instance.conversionCompatibleValue(test.getField("datetime"), test.getField("date"), CalendarUtils.now()));
	}
	
	@Test
	public void testGetFillinValue() throws Exception {
		Entity test = MetadataHelper.getEntity(TEST_ENTITY);
		Field referenceField = test.getField("reference");
		
		System.out.println(
				AutoFillinManager.instance.getFillinValue(referenceField, ID.newId(test.getEntityCode())));
	}
}
