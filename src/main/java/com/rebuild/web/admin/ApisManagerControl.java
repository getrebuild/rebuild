/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.RebuildApiService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author devezhao
 * @since 2019/7/22
 */
@RequestMapping("/admin/")
@Controller
public class ApisManagerControl extends BaseController {

    @GetMapping("apis-manager")
    public ModelAndView pageManager() {
        return createModelAndView("/admin/integration/apis-manager");
    }

    @RequestMapping("apis-manager/app-list")
    public void appList(HttpServletResponse response) {
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
    public void appCreate(HttpServletRequest request, HttpServletResponse response) {
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
    public void appDelete(HttpServletRequest request, HttpServletResponse response) {
        ID id = getIdParameterNotNull(request, "id");
        Application.getBean(RebuildApiService.class).delete(id);
        writeSuccess(response);
    }
}
