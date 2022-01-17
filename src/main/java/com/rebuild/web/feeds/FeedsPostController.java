/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

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
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.feeds.FeedsType;
import com.rebuild.core.support.KVStorage;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 操作相关
 *
 * @author devezhao
 * @since 2019/11/1
 */
@RestController
@RequestMapping("/feeds/post/")
public class FeedsPostController extends BaseController {

    @PostMapping({"publish", "comment"})
    public JSON publish(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        JSON formJson = ServletUtils.getRequestJson(request);
        Record record = EntityHelper.parse((JSONObject) formJson, user);

        Application.getService(record.getEntity().getEntityCode()).createOrUpdate(record);
        return JSONUtils.toJSONObject("id", record.getPrimary());
    }

    @RequestMapping("like")
    public RespBody like(HttpServletRequest request, @IdParam ID source) {
        final ID user = getRequestUser(request);

        Object[] liked = Application.createQueryNoFilter(
                "select likeId from FeedsLike where source = ? and createdBy = ?")
                .setParameter(1, source)
                .setParameter(2, user)
                .unique();
        if (liked == null) {
            Record record = EntityHelper.forNew(EntityHelper.FeedsLike, user);
            record.setID("source", source);
            Application.getCommonsService().create(record);
        } else {
            Application.getCommonsService().delete((ID) liked[0]);
        }

        return RespBody.ok(liked == null);
    }

    @RequestMapping("delete")
    public RespBody delete(@IdParam ID anyFeedsId) {
        Application.getService(anyFeedsId.getEntityCode()).delete(anyFeedsId);
        return RespBody.ok();
    }

    @RequestMapping("finish-schedule")
    public RespBody finishSchedule(HttpServletRequest request, @IdParam ID feedsId) {
        final ID user = getRequestUser(request);

        Object[] schedule = Application.createQueryNoFilter(
                "select createdBy,contentMore from Feeds where feedsId = ? and type = ?")
                .setParameter(1, feedsId)
                .setParameter(2, FeedsType.SCHEDULE.getMask())
                .unique();
        if (schedule == null || !schedule[0].equals(user)) {
            return RespBody.error(Language.L("无操作权限"));
        }

        // 非结构化存储
        JSONObject contentMore = JSON.parseObject((String) schedule[1]);
        contentMore.put("finishTime", CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now()));

        Record record = EntityHelper.forUpdate(feedsId, user);
        record.setString("contentMore", contentMore.toJSONString());
        record.removeValue(EntityHelper.ModifiedOn);
        Application.getCommonsService().update(record);

        return RespBody.ok();
    }

    @PostMapping("feeds-top")
    public RespBody feedsTop(@IdParam(required = false) ID feedsId, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        // null 表示取消
        KVStorage.setCustomValue("FEEDS-TOP:" + user, ObjectUtils.defaultIfNull(feedsId, null));
        return RespBody.ok();
    }
}
