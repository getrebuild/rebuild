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

package com.rebuild.web.base.configuration;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.portals.BaseLayoutManager;
import com.rebuild.server.configuration.portals.ShareToManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.configuration.LayoutConfigService;
import com.rebuild.web.BaseControll;
import com.rebuild.web.PortalsConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao
 * @since 2019/10/18
 */
@Controller
@RequestMapping("/app/{entity}/")
public class WidgetControll extends BaseControll implements PortalsConfiguration {

    @RequestMapping(value = "/widget-charts", method = RequestMethod.POST)
    @Override
    public void sets(@PathVariable String entity,
                     HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        JSON config = ServletUtils.getRequestJson(request);
        ID cfgid = getIdParameter(request, "id");
        if (cfgid != null && !ShareToManager.isSelf(user, cfgid)) {
            cfgid = null;
        }

        Record record;
        if (cfgid == null) {
            record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
            record.setString("belongEntity", entity);
            record.setString("applyType", BaseLayoutManager.TYPE_WCHARTS);
            record.setString("shareTo", BaseLayoutManager.SHARE_SELF);
        } else {
            record = EntityHelper.forUpdate(cfgid, user);
        }
        record.setString("config", config.toJSONString());
        putCommonsFields(request, record);
        record = Application.getBean(LayoutConfigService.class).createOrUpdate(record);

        writeSuccess(response, record.getPrimary());
    }

    @RequestMapping(value = "/widget-charts", method = RequestMethod.GET)
    @Override
    public void gets(@PathVariable String entity,
                     HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        ConfigEntry config = BaseLayoutManager.instance.getWidgetOfCharts(user, entity);
        writeSuccess(response, config == null ? null : config.toJSON());
    }
}
