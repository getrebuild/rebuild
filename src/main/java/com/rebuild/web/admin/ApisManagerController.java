/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.RebuildApiService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * @author devezhao
 * @since 2019/7/22
 */
@RestController
@RequestMapping("/admin/")
public class ApisManagerController extends BaseController {

    @GetMapping("apis-manager")
    public ModelAndView pageManager() {
        return createModelAndView("/admin/integration/apis-manager");
    }

    @GetMapping("apis-manager/app-list")
    public RespBody appList() {
        Object[][] apps = Application.createQueryNoFilter(
                "select uniqueId,appId,appSecret,bindUser,bindUser.fullName,createdOn,appId,bindIps from RebuildApi order by createdOn")
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
            o[5] = I18nUtils.formatDate((Date) o[5]);
        }

        return RespBody.ok(apps);
    }

    @PostMapping("apis-manager/reset-secret")
    public RespBody resetSecret(HttpServletRequest request) {
        ID appId = getIdParameterNotNull(request, "id");
        Record record = EntityHelper.forUpdate(appId, getRequestUser(request));
        record.setString("appSecret", CodecUtils.randomCode(40));
        Application.getCommonsService().update(record, false);

        // cache
        Application.getBean(RebuildApiService.class).update(record);
        return RespBody.ok();
    }
}
