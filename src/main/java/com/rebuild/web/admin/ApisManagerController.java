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
import com.rebuild.core.support.CommonsLog;
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
import java.util.HashMap;
import java.util.Map;

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
                "select uniqueId,appId,appSecret,bindUser,bindUser.fullName,createdOn,bindIps from RebuildApi order by createdOn")
                .array();
        for (Object[] o : apps) {
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

    @GetMapping("apis-manager/request-times")
    public RespBody requestTimes(HttpServletRequest request) {
        final String appids = getParameterNotNull(request, "appid");

        Map<String, Object[]> times = new HashMap<>();
        for (String appid : appids.split("[,;]")) {
            Object[] count = Application.createQueryNoFilter(
                    "select count(requestId),max(requestTime) from RebuildApiRequest where appId = ? and requestTime > ?")
                    .setParameter(1, appid)
                    .setParameter(2, CalendarUtils.addDay(-SHOW_DAYS))
                    .unique();
            // v4.2
            if (count[1] != null) I18nUtils.formatDate((Date) count[1]);
            times.put(appid, count);
        }
        return RespBody.ok(times);
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

    // -- v4.5 for DataSubscribe

    @GetMapping("apis-manager/subscribe")
    public ModelAndView pageDataSubscribe() {
        return createModelAndView("/admin/integration/data-subscribe-list");
    }

    @GetMapping("apis-manager/subscribe-push-logs")
    public RespBody subscribePushLogs(HttpServletRequest request) {
        ID source = getIdParameterNotNull(request, "id");
        String q = getParameter(request, "q");
        int pageNo = getIntParameter(request, "pn", 1);
        int pageSize = 40;

        String sql = "select logTime,logContent,status,logId from CommonsLog" +
                " where type = ? and source = ? and logTime > ? and (1=1) order by logTime desc";
        if (StringUtils.isNotBlank(q)) {
            q = CommonsUtils.escapeSql(q);
            sql = sql.replace("(1=1)", String.format("logContent like '%%%s%%'", q));
        }

        Object[][] array = Application.createQueryNoFilter(sql)
                .setParameter(1, CommonsLog.TYPE_TRIGGER)
                .setParameter(2, source)
                .setParameter(3, CalendarUtils.addDay(-SHOW_DAYS))
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        for (Object[] o : array) {
            o[0] = I18nUtils.formatDate((Date) o[0]);
        }
        return RespBody.ok(array);
    }

    @GetMapping("apis-manager/subscribe-push-times")
    public RespBody subscribePushTimes(HttpServletRequest request) {
        final String ids = getParameterNotNull(request, "id");

        Map<String, Object[]> times = new HashMap<>();
        for (String id : ids.split("[,;]")) {
            if (!ID.isId(id)) continue;
            Object[] count = Application.createQueryNoFilter(
                    "select count(logId),max(logTime) from CommonsLog where type = ? and source = ? and logTime > ?")
                    .setParameter(1, CommonsLog.TYPE_TRIGGER)
                    .setParameter(2, ID.valueOf(id))
                    .setParameter(3, CalendarUtils.addDay(-SHOW_DAYS))
                    .unique();
            if (count[1] != null) count[1] = I18nUtils.formatDate((Date) count[1]);
            times.put(id, count);
        }
        return RespBody.ok(times);
    }
}
