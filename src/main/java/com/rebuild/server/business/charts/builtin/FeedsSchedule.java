/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.charts.builtin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.momentjava.Moment;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.charts.ChartData;
import com.rebuild.server.service.notification.MessageBuilder;
import com.rebuild.utils.JSONUtils;

import java.util.Date;

/**
 * 代办日程
 *
 * @author devezhao
 * @since 2020/2/28
 */
public class FeedsSchedule  extends ChartData implements BuiltinChart {

    // 虚拟ID
    public static final ID MYID = ID.valueOf("017-9000000000000002");

    public FeedsSchedule() {
        super(null);
        this.config = getChartConfig();
    }

    @Override
    public ID getChartId() {
        return MYID;
    }

    @Override
    public String getChartTitle() {
        return "动态日程";
    }

    @Override
    public JSONObject getChartConfig() {
        return JSONUtils.toJSONObject(new String[]{"entity", "type"}, new String[]{"Feeds", getChartType()});
    }

    @Override
    public JSON build() {
        Object[][] array = Application.createQueryNoFilter(
                "select feedsId,scheduleTime,content,contentMore from Feeds" +
                        " where createdBy = ? and type = 4 and scheduleTime > ? order by scheduleTime")
                .setParameter(1, getUser())
                .setParameter(2, CalendarUtils.addDay(0))
                .setLimit(200)
                .array();

        final long nowTime = CalendarUtils.now().getTime();
        JSONArray list = new JSONArray();
        for (Object[] o : array) {
            final Date date = (Date) o[1];
            String scheduleTime = CalendarUtils.getUTCDateTimeFormat().format(date).substring(0, 16);
            String fromNow = Moment.moment(date).fromNow();
            if (nowTime > date.getTime()) {
                fromNow = "-" + fromNow;
            }

            String content = (String) o[2];
            content = MessageBuilder.formatMessage(content);

            Integer state = JSON.parseObject((String) o[3]).getInteger("scheduleState");
            if (state == null) state = 0;

            JSONObject item = JSONUtils.toJSONObject(
                    new String[] { "id", "scheduleTime", "scheduleLeft", "content", "state" },
                    new Object[] { o[0], scheduleTime, fromNow, content, state });
            list.add(item);
        }

        return list;
    }
}
