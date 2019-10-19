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

import cn.devezhao.persist4j.Record;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.configuration.WidgetConfigService;
import com.rebuild.utils.JSONUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/10/18
 */
public class WidgetManagerTest extends TestSupport {

    @Before
    public void setUp() throws Exception {
        Application.getSessionStore().set(SIMPLE_USER);
    }
    @After
    public void tearDown() throws Exception {
        Application.getSessionStore().clean();
    }

    @Test
    public void getDataListChart() {
        Record config = EntityHelper.forNew(EntityHelper.WidgetConfig, SIMPLE_USER);
        config.setString("config", JSONUtils.EMPTY_ARRAY_STR);
        config.setString("belongEntity", TEST_ENTITY);
        config.setString("applyType", WidgetManager.TYPE_DATALIST);
        config = Application.getBean(WidgetConfigService.class).createOrUpdate(config);

        ConfigEntry configEntry = WidgetManager.instance.getDataListChart(UserService.ADMIN_USER, TEST_ENTITY);
        System.out.println(configEntry);

        Application.getBean(WidgetConfigService.class).delete(config.getPrimary());
        configEntry = WidgetManager.instance.getDataListChart(UserService.ADMIN_USER, TEST_ENTITY);
        System.out.println(configEntry);
    }
}