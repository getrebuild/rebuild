/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.service.AibotConfigManager;
import com.rebuild.core.aibot2.service.AibotConfigService;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

import static com.rebuild.core.aibot2.service.AibotConfigManager.TYPE_AIBOT_USERMEMORY;

/**
 * 用户记忆
 *
 * @author devezhao
 * @since 2026/8/13
 */
@Slf4j
public class UserMemory implements Tool {

    // 用户记忆使用指引
    public static final String MEMORY_GUIDANCE =
            "通过 UserMemory 工具为用户维护长期个性化记忆：\n" +
                    "- 当用户告知长期有效的身份/偏好信息，或明确要求\"记住/忘记\"时，主动调用 UserMemory 记录或删除，并根据信息重要程度选择档位（1/2/3）\n" +
                    "- 一次性、仅本次会话特有的信息不要记录；次要信息记为第 3 档，记忆满额时会最先被自动移除\n" +
                    "- update/delete 前先通过 list 查看现有记忆，通过记忆内容定位目标条目并向用户确认后再操作\n" +
                    "- 注入的记忆仅供参考，不要主动向用户复述";

    private static final int MAX_MEMORIES = 30;
    private static final int MAX_CONTENT_LENGTH = 150;

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String action = args.getString("action");
        if (StringUtils.isBlank(action)) {
            throw new ToolException("action 参数不能为空，可选值: add/update/delete/list");
        }

        ID user = UserContextHolder.getUser();
        String message;
        switch (action) {
            case "add": {
                String content = args.getString("content");
                if (StringUtils.isBlank(content)) {
                    throw new ToolException("add 操作必须提供 content 参数（记忆内容）");
                }
                Integer level = args.getInteger("level");
                message = add(user, content, level == null ? 2 : level);
                break;
            }
            case "update": {
                String oldContent = args.getString("oldContent");
                if (StringUtils.isBlank(oldContent)) {
                    throw new ToolException("update 操作必须提供 oldContent 参数（要修改的现有记忆内容，可先通过 list 查看）");
                }
                String content = args.getString("content");
                if (StringUtils.isBlank(content)) {
                    throw new ToolException("update 操作必须提供 content 参数（新内容）");
                }
                message = update(user, oldContent, content, args.getInteger("level"));
                break;
            }
            case "delete": {
                String content = args.getString("content");
                if (StringUtils.isBlank(content)) {
                    throw new ToolException("delete 操作必须提供 content 参数（要删除的记忆内容，可先通过 list 查看）");
                }
                message = delete(user, content);
                break;
            }
            case "list": {
                message = list(user);
                break;
            }
            default:
                throw new ToolException("无效的 action: " + action + "，可选值: add/update/delete/list");
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "message"},
                new Object[]{"ok", message});
    }

    /**
     * @param user
     * @param content
     * @param level
     * @return
     */
    private String add(ID user, String content, int level) {
        content = CommonsUtils.maxstr(StringUtils.trim(content), MAX_CONTENT_LENGTH);

        Record r = EntityHelper.forNew(EntityHelper.AibotConfig, user);
        r.setString("type", TYPE_AIBOT_USERMEMORY);
        r.setString("config", newMemoryConfig(content, level).toJSONString());
        Application.getBean(AibotConfigService.class).create(r);

        // 超限自动移除：level 数值最大（最低档）者优先，同档按 createdOn 最旧者优先
        StringBuilder removed = new StringBuilder();
        ConfigBean[] memories = AibotConfigManager.instance.getUserMemoryConfigs(user);
        while (memories.length > MAX_MEMORIES) {
            ConfigBean worst = null;
            for (ConfigBean cb : memories) {
                if (worst == null
                        || cb.getInteger("level") > worst.getInteger("level")
                        || (cb.getInteger("level").equals(worst.getInteger("level"))
                        && ((Date) cb.getObject("createdOn")).before((Date) worst.getObject("createdOn")))) {
                    worst = cb;
                }
            }

            Application.getBean(AibotConfigService.class).delete(worst.getID("id"));
            if (removed.length() > 0) removed.append("；");
            removed.append(worst.getString("content"));

            memories = AibotConfigManager.instance.getUserMemoryConfigs(user);
        }

        String result = String.format("已记住（第 %d 档）：%s", level, content);
        if (removed.length() > 0) {
            result += String.format("\n注意：记忆数量已达上限（%d 条），已自动移除最低档位的记忆：%s", MAX_MEMORIES, removed);
        }
        return result;
    }

    /**
     * 按旧内容定位并修改记忆（找到多条取最新一条）
     *
     * @param user
     * @param oldContent
     * @param content
     * @param level
     * @return
     */
    private String update(ID user, String oldContent, String content, Integer level) {
        oldContent = StringUtils.trim(oldContent);
        if (StringUtils.isBlank(content)) {
            return "更新失败：请提供新内容 (content)";
        }

        ConfigBean target = null;
        for (ConfigBean cb : AibotConfigManager.instance.getUserMemoryConfigs(user)) {
            if (oldContent.equals(cb.getString("content"))) {
                target = cb;
                break;
            }
        }
        if (target == null) {
            return String.format("未找到内容为「%s」的记忆，请先通过 list 查看当前记忆", oldContent);
        }

        JSONObject config = newMemoryConfig(
                CommonsUtils.maxstr(StringUtils.trim(content), MAX_CONTENT_LENGTH),
                level != null ? level : target.getInteger("level"));

        Record r = EntityHelper.forUpdate(target.getID("id"), user, false);
        r.setString("config", config.toJSONString());
        Application.getBean(AibotConfigService.class).update(r);

        return String.format("已更新记忆：%s", target.getString("content"));
    }

    /**
     * 按内容匹配删除记忆（找到多条全部删除）
     *
     * @param user
     * @param content
     * @return
     */
    private String delete(ID user, String content) {
        content = StringUtils.trim(content);

        int deleted = 0;
        for (ConfigBean cb : AibotConfigManager.instance.getUserMemoryConfigs(user)) {
            if (content.equals(cb.getString("content"))) {
                Application.getBean(AibotConfigService.class).delete(cb.getID("id"));
                deleted++;
            }
        }
        if (deleted == 0) {
            return String.format("未找到内容为「%s」的记忆，请先通过 list 查看当前记忆", content);
        }

        return String.format("已删除记忆：%s", content);
    }

    /**
     * @param user
     * @return
     */
    private String list(ID user) {
        ConfigBean[] memories = AibotConfigManager.instance.getUserMemoryConfigs(user);
        if (memories.length == 0) return "暂无用户记忆";

        StringBuilder sb = new StringBuilder(String.format("当前用户记忆（共 %d 条，按记录时间由新到旧）：\n", memories.length));
        for (int i = 0; i < memories.length; i++) {
            ConfigBean cb = memories[i];
            sb.append(String.format("%d. [第 %d 档] %s",
                    i + 1, cb.getInteger("level"), cb.getString("content")));
            if (i < memories.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * @param content
     * @param level
     * @return
     */
    private static JSONObject newMemoryConfig(String content, int level) {
        if (level < 1) level = 1;
        else if (level > 3) level = 3;

        JSONObject config = new JSONObject();
        config.put("content", content);
        config.put("level", level);
        return config;
    }
}
