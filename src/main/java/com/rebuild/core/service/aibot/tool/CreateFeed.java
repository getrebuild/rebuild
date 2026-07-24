/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.feeds.FeedsService;
import com.rebuild.core.service.feeds.FeedsType;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * 新建动态
 *
 * @author devezhao
 * @since 2026/7/24
 */
@Slf4j
public class CreateFeed implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        JSONObject args = StringUtils.isBlank(arguments) ? new JSONObject() : JSON.parseObject(arguments);

        String content = args.getString("content");
        if (StringUtils.isBlank(content)) {
            throw new ToolException("动态内容 (content) 不能为空");
        }

        int type = args.getIntValue("type");
        if (type < 1) type = FeedsType.ACTIVITY.getMask();
        // 不允许通过 AI 创建公告
        if (type == FeedsType.ANNOUNCEMENT.getMask()) {
            throw new ToolException("公告仅管理员可在前端发布，不支持通过 AI 创建");
        }
        // 校验类型有效性
        if (type != FeedsType.ACTIVITY.getMask()
                && type != FeedsType.FOLLOWUP.getMask()
                && type != FeedsType.SCHEDULE.getMask()) {
            throw new ToolException("无效的动态类型 (type)，可选值: 1=动态, 2=跟进, 4=日程");
        }

        final ID user = UserContextHolder.getUser();

        Record record = EntityHelper.forNew(EntityHelper.Feeds, user);
        record.setInt("type", type);
        record.setString("content", content);

        // 可见范围
        String scope = args.getString("scope");
        if (StringUtils.isNotBlank(scope)) {
            record.setString("scope", scope);
        }

        // 跟进：关联记录
        if (type == FeedsType.FOLLOWUP.getMask()) {
            String relatedRecord = args.getString("relatedRecord");
            if (StringUtils.isNotBlank(relatedRecord) && ID.isId(relatedRecord)) {
                record.setID("relatedRecord", ID.valueOf(relatedRecord));
            }
        }

        // 日程：设置时间和提醒
        if (type == FeedsType.SCHEDULE.getMask()) {
            String scheduleTime = args.getString("scheduleTime");
            Date scheduleDate;
            if (StringUtils.isNotBlank(scheduleTime)) {
                scheduleDate = CalendarUtils.parse(scheduleTime);
            } else {
                // 默认: 当前时间+1D
                scheduleDate = CalendarUtils.addDay(1);
            }
            record.setDate("scheduleTime", scheduleDate);

            String timeStr = CalendarUtils.getUTCDateTimeFormat().format(scheduleDate).substring(0, 16);
            JSONObject contentMore = JSONUtils.toJSONObject(
                    new String[]{"scheduleTime", "scheduleRemind"},
                    new Object[]{timeStr, 1});
            record.setString("contentMore", contentMore.toJSONString());
        }

        record = Application.getBean(FeedsService.class).create(record);

        String typeName = FeedsType.parse(type).getName();
        return JSONUtils.toJSONObject(
                new String[]{"status", "id", "message"},
                new Object[]{"ok", record.getPrimary().toLiteral(),
                        String.format("已成功发布%s，ID: %s", typeName, record.getPrimary())});
    }
}
