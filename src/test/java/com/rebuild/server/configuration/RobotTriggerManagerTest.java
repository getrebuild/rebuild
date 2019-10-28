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

import cn.devezhao.persist4j.Entity;
import com.rebuild.server.TestSupport;
import com.rebuild.server.business.trigger.TriggerAction;
import com.rebuild.server.business.trigger.TriggerWhen;
import com.rebuild.server.metadata.MetadataSorter;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/27
 */
public class RobotTriggerManagerTest extends TestSupport {
	
	@Test
	public void testGetActionsByEntity() throws Exception {
		for (Entity entity : MetadataSorter.sortEntities()) {
			RobotTriggerManager.instance.clean(entity);
			TriggerAction actions[] = RobotTriggerManager.instance.getActions(
					entity, TriggerWhen.CREATE, TriggerWhen.ASSIGN);
			if (actions.length > 0) {
				System.out.println("TriggerAction on " + entity.getName() + " ... " + actions.length);
				for (TriggerAction a : actions) {
					System.out.println(a);
				}
			}
		}
	}
}
