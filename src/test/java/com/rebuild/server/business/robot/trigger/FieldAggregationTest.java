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

package com.rebuild.server.business.robot.trigger;

import org.junit.Test;

import com.rebuild.server.TestSupport;
import com.rebuild.server.business.robot.TriggerAction;
import com.rebuild.server.business.robot.TriggerWhen;
import com.rebuild.server.configuration.RobotTriggerManager;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * TODO
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/29
 */
public class FieldAggregationTest extends TestSupport {

	@Test
	public void test() throws Exception {
		Entity test = MetadataHelper.getEntity("dingdanmingxi");
		RobotTriggerManager.instance.clean(test);
		
		TriggerAction[] as = RobotTriggerManager.instance.getActions(ID.newId(test.getEntityCode()), TriggerWhen.CREATE);
		for (TriggerAction action : as) {
			action.execute(null);
		}
	}
}
