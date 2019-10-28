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

package com.rebuild.web.admin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.configuration.RebuildApiService;
import com.rebuild.web.BasePageControll;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao
 * @since 2019/7/22
 */
@RequestMapping("/admin/")
@Controller
public class ApisManagerControll extends BasePageControll {

    @RequestMapping("apis-manager")
    public ModelAndView pageManager(HttpServletRequest request) throws IOException {
        return createModelAndView("/admin/integration/apis-manager.jsp");
    }

    @RequestMapping("apis-manager/app-list")
    public void appList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object[][] apps = Application.createQueryNoFilter(
                "select uniqueId,appId,appSecret,bindUser,bindUser.fullName,createdOn,appId from RebuildApi")
                .array();

        // 近30日用量
        for (Object[] o : apps) {
            String appid = (String) o[6];
            Object[] count = Application.createQueryNoFilter(
                    "select count(requestId) from RebuildApiRequest where appId = ? and requestTime > ?")
                    .setParameter(1, appid)
                    .setParameter(2, CalendarUtils.addDay(-30))
                    .unique();
            o[6] = count[0];
        }

        writeSuccess(response, apps);
    }

    @RequestMapping("apis-manager/app-create")
    public void appCreate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        ID bindUser = getIdParameter(request, "bind");

        Record record = EntityHelper.forNew(EntityHelper.RebuildApi, user);
        record.setString("appId", (100000000 + RandomUtils.nextInt(899999999)) + "");
        record.setString("appSecret", CodecUtils.randomCode(40));
        record.setID("bindUser", bindUser);
        Application.getBean(RebuildApiService.class).create(record);
        writeSuccess(response);
    }

    @RequestMapping("apis-manager/app-delete")
    public void appDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID id = getIdParameterNotNull(request, "id");
        Application.getBean(RebuildApiService.class).delete(id);
        writeSuccess(response);
    }
}
