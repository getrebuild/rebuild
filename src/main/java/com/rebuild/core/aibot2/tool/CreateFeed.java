/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.feeds.FeedsScope;
import com.rebuild.core.service.feeds.FeedsService;
import com.rebuild.core.service.feeds.FeedsType;
import com.rebuild.utils.CommonsUtils;
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
        final JSONObject args = JSON.parseObject(arguments);

        String content = args.getString("content");
        if (StringUtils.isBlank(content)) {
            throw new KnownToolException("动态内容 (content) 不能为空");
        }

        int type = args.getIntValue("type");
        if (type < 1) type = FeedsType.ACTIVITY.getMask();
        // 不允许通过 AI 创建公告
        if (type == FeedsType.ANNOUNCEMENT.getMask()) {
            throw new KnownToolException("公告仅管理员可在前端发布，不支持通过 AI 创建。可选类型: 1=动态, 2=跟进, 4=日程");
        }
        if (type != FeedsType.ACTIVITY.getMask()
                && type != FeedsType.FOLLOWUP.getMask()
                && type != FeedsType.SCHEDULE.getMask()) {
            throw new KnownToolException("无效的动态类型 (type)，可选值: 1=动态, 2=跟进, 4=日程");
        }

        Record record = EntityHelper.forNew(EntityHelper.Feeds, UserContextHolder.getUser());
        record.setInt("type", type);
        record.setString("content", content);

        record.setString("scope", resolveScope(args.getString("scope")));

        // 图片（支持单个 fileKey 字符串或数组）
        String imagesStr = ToolHelper.resolveFileKeys(args.get("images"));
        if (imagesStr != null) {
            record.setString("images", imagesStr);
        }

        // 附件（支持单个 fileKey 字符串或数组）
        String attachmentsStr = ToolHelper.resolveFileKeys(args.get("attachments"));
        if (attachmentsStr != null) {
            record.setString("attachments", attachmentsStr);
        }

        // 跟进：必须关联记录
        if (type == FeedsType.FOLLOWUP.getMask()) {
            if (StringUtils.isBlank(args.getString("relatedRecordId"))) {
                throw new KnownToolException("跟进动态必须关联业务记录 (relatedRecordId)，可通过 QueryRecords 查询获取");
            }
            ID relatedRecordId = ToolHelper.resolveId(args.getString("relatedRecordId"), "relatedRecordId");
            record.setID("relatedRecord", relatedRecordId);
        }

        if (type == FeedsType.SCHEDULE.getMask()) {
            String scheduleTime = args.getString("scheduleTime");
            Date scheduleDate;
            if (StringUtils.isNotBlank(scheduleTime)) {
                scheduleDate = CommonsUtils.parseDate(scheduleTime);
                if (scheduleDate == null) {
                    throw new KnownToolException("无法解析日程时间: " + scheduleTime + "，请使用 yyyy-MM-dd HH:mm:ss 格式");
                }
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

    /**
     * 校验并规范化可见范围。scope 直接落库，非法值会让该条动态在后续读取（FeedsHelper.checkReadable）时抛异常
     *
     * @param scope
     * @return
     */
    private String resolveScope(String scope) {
        scope = StringUtils.trimToNull(scope);
        if (scope == null) {
            return FeedsScope.ALL.name();  // 与前端发布动态的默认可见范围一致
        }

        if (ID.isId(scope)) {
            ID teamId = ID.valueOf(scope);
            if (teamId.getEntityCode() != EntityHelper.Team) {
                throw new KnownToolException("scope 为团队 ID 时必须是团队 (Team) 的 ID : " + scope);
            }
            if (!Application.getUserStore().existsAny(teamId)) {
                throw new KnownToolException("团队不存在 : " + scope);
            }
            Team team = Application.getUserStore().getTeam(teamId);
            if (!team.isMember(UserContextHolder.getUser())) {
                throw new KnownToolException("你不是团队 [" + team.getName() + "] 的成员，无法向其发布动态");
            }
            return teamId.toLiteral();
        }

        String parsed;
        try {
            parsed = FeedsScope.parse(scope).name();
        } catch (IllegalArgumentException ex) {
            throw new KnownToolException("无效的可见范围 (scope) : " + scope + "，可用值: ALL(公开), SELF(私密), 或团队 ID");
        }
        if (FeedsScope.GROUP.name().equals(parsed)) {
            throw new KnownToolException("scope 为团队可见时必须传团队 ID，而非 GROUP 字面值");
        }
        return parsed;
    }
}
