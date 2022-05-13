/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import cn.devezhao.persist4j.Entity;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataSorter;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/27
 */
public class RobotTriggerManagerTest extends TestSupport {

    @Test
    public void testGetActionsByEntity() {
        for (Entity entity : MetadataSorter.sortEntities()) {
            RobotTriggerManager.instance.clean(entity);

            TriggerAction[] actions = RobotTriggerManager.instance.getActions(
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
