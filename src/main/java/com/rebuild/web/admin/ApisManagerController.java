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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.RebuildApiService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang3.StringUtils;
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

    private static final int SHOW_DAYS = 90;

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
                    .setParameter(2, CalendarUtils.addDay(-SHOW_DAYS))
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

    @GetMapping("apis-manager/request-logs")
    public RespBody requestLogs(HttpServletRequest request) {
        String appid = getParameterNotNull(request, "appid");
        String q = getParameter(request, "q");
        int pageNo = getIntParameter(request, "pn", 1);
        int pageSize = 40;

        String sql = "select remoteIp,requestTime,responseTime,requestUrl,requestBody,responseBody,requestId from RebuildApiRequest" +
                " where appId = ? and requestTime > ? and (1=1) order by requestTime desc";
        if (StringUtils.isNotBlank(q)) {
            q = CommonsUtils.escapeSql(q);
            // https://zhuanlan.zhihu.com/p/35675553
            if (CommandArgs.getBoolean(CommandArgs._UseDbFullText)) {
                sql = sql.replace("(1=1)", String.format("(requestBody match '%s' or responseBody match '%s')", q, q));
            } else {
                sql = sql.replace("(1=1)", String.format("(requestBody like '%%%s%%' or responseBody like '%%%s%%')", q, q));
            }
        }

        Object[][] array = Application.createQueryNoFilter(sql)
                .setParameter(1, appid)
                .setParameter(2, CalendarUtils.addDay(-SHOW_DAYS))
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        for (Object[] o : array) {
            o[1] = I18nUtils.formatDate((Date) o[1]);
            o[2] = I18nUtils.formatDate((Date) o[2]);

            final String resp = (String) o[5];
            try {
                o[4] = JSON.parse((String) o[4]);
                o[5] = JSON.parse(resp.substring(37));
            } catch (JSONException ignored) {
                o[5] = resp.substring(37);
            }
            o[6] = resp.substring(0, 36);  // request-id
        }

        return RespBody.ok(array);
    }
}
