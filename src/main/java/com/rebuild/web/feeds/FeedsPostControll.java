/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.feeds;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.feeds.FeedsType;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 操作相关
 *
 * @author devezhao
 * @since 2019/11/1
 */
@Controller
@RequestMapping("/feeds/post/")
public class FeedsPostControll extends BaseControll {

    @RequestMapping({"publish", "comment"})
    public void publish(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        JSON formJson = ServletUtils.getRequestJson(request);
        Record record = EntityHelper.parse((JSONObject) formJson, user);
        String content = record.getString("content");
        record.setString("content", CommonsUtils.escapeHtml(content));

        Application.getService(record.getEntity().getEntityCode()).createOrUpdate(record);
        JSON ret = JSONUtils.toJSONObject("id", record.getPrimary());
        writeSuccess(response, ret);
    }

    @RequestMapping("like")
    public void like(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        ID source = getIdParameterNotNull(request, "id");

        Object[] liked = Application.createQueryNoFilter(
                "select likeId from FeedsLike where source = ? and createdBy = ?")
                .setParameter(1, source)
                .setParameter(2, user)
                .unique();
        if (liked == null) {
            Record record = EntityHelper.forNew(EntityHelper.FeedsLike, user);
            record.setID("source", source);
            Application.getCommonService().create(record);
        } else {
            Application.getCommonService().delete((ID) liked[0]);
        }

        writeSuccess(response, liked == null);
    }

    @RequestMapping("delete")
    public void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID anyId = getIdParameterNotNull(request, "id");
        Application.getService(anyId.getEntityCode()).delete(anyId);
        writeSuccess(response);
    }

    @RequestMapping("finish-schedule")
    public void finishSchedule(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID user = getRequestUser(request);
        final ID feedsId = getIdParameterNotNull(request, "id");

        Object[] schedule = Application.createQueryNoFilter(
                "select createdBy,contentMore from Feeds where feedsId = ? and type = ?")
                .setParameter(1, feedsId)
                .setParameter(2, FeedsType.SCHEDULE.getMask())
                .unique();
        if (schedule == null || !schedule[0].equals(user)) {
            writeFailure(response, "无权操作他人日程");
            return;
        }

        // 非结构化存储
        JSONObject contentMore = JSON.parseObject((String) schedule[1]);
        contentMore.put("finishTime", CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now()));

        Record record = EntityHelper.forUpdate(feedsId, user);
        record.setString("contentMore", contentMore.toJSONString());
        record.removeValue(EntityHelper.ModifiedOn);
        Application.getCommonService().update(record);
        writeSuccess(response);
    }
}
