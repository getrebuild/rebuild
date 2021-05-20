/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.feeds;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.feeds.FeedsHelper;
import com.rebuild.core.service.feeds.FeedsScope;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 动态公告
 *
 * @author devezhao
 * @since 2019/12/19
 */
@RestController
public class AnnouncementController extends BaseController {

    @GetMapping("/commons/announcements")
    public RespBody list(HttpServletRequest request) {
        final ID user = AppUtils.getRequestUser(request);
        int fromWhere = getIntParameter(request, "from", 0);

        if (fromWhere == 0) {
            // 1=动态页 2=首页 4=登录页
            String refererUrl = StringUtils.defaultIfBlank(request.getHeader("Referer"), "");
            if (refererUrl.contains("/user/login")) {
                fromWhere = 4;
            } else if (refererUrl.contains("/dashboard/home")) {
                fromWhere = 2;
            } else if (refererUrl.contains("/feeds/")) {
                fromWhere = 1;
            }
        }

        if (fromWhere == 0) return RespBody.ok();

        Object[][] array = Application.createQueryNoFilter(
                "select content,contentMore,scope,createdBy,createdOn,feedsId from Feeds where type = 3")
                .array();

        List<JSON> as = new ArrayList<>();
        long timeNow = CalendarUtils.now().getTime();
        for (Object[] o : array) {
            JSONObject options = JSON.parseObject((String) o[1]);

            // 不在指定位置

            int whereMask = options.getIntValue("showWhere");
            if ((fromWhere & whereMask) == 0) {
                continue;
            }

            // 不在展示时间
            Date timeStart = parseTime(options.getString("timeStart"));
            if (timeStart != null && timeNow < timeStart.getTime()) {
                continue;
            }
            Date timeEnd = parseTime(options.getString("timeEnd"));
            if (timeEnd != null && timeNow > timeEnd.getTime()) {
                continue;
            }

            // 不可见
            boolean allow = false;
            String scope = (String) o[2];
            if (FeedsScope.ALL.name().equalsIgnoreCase(scope)) {
                allow = true;
            } else if (FeedsScope.SELF.name().equalsIgnoreCase(scope) && o[3].equals(user)) {
                allow = true;
            } else if (ID.isId(scope) && user != null) {
                ID teamId = ID.valueOf(scope);
                if (Application.getUserStore().existsAny(teamId)) {
                    allow = Application.getUserStore().getTeam(teamId).isMember(user);
                }
            }

            if (allow) {
                JSONObject a = new JSONObject();
                a.put("content", FeedsHelper.formatContent((String) o[0], true));
                a.put("publishOn", CalendarUtils.getUTCDateTimeFormat().format(o[4]).substring(0, 16));
                a.put("publishBy", UserHelper.getName((ID) o[3]));
                a.put("id", o[5]);
                as.add(a);
            }
        }

        return RespBody.ok(as);
    }

    private Date parseTime(String time) {
        if (StringUtils.isBlank(time)) return null;
        return CalendarUtils.parse(time + (time.length() == 16 ? ":00" : ""));
    }
}
