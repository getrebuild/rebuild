/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 需二次确认的工具标记，并集中承载确认草稿处理（schema 注入、草稿缓存与恢复）。
 * 调用点见 ToolDefs#getToolJson 与 ToolDefs#execute。
 *
 * @author Zixin
 * @since 2026-10-18
 */
public interface ConfirmableTool {

    // 草稿缓存时长（秒）
    int DRAFT_TTL = CommonsCache.TS_DAY * 3;

    // 草稿缓存 key 前缀
    String DRAFT_KEY_PREFIX = "AIBOT-CONFIRM-DRAFT:";

    /**
     * 注入 confirmed 与 draftId 参数声明，模型与 MCP 两端 schema 共用此出口。非 ConfirmableTool 不处理。
     *
     * @param json
     * @param tool
     */
    static void decorate(JSONObject json, Tool tool) {
        if (json == null || !(tool instanceof ConfirmableTool)) return;

        JSONObject funcJson = json.getJSONObject("function");
        JSONObject paramsJson = funcJson == null ? null : funcJson.getJSONObject("parameters");
        JSONObject props = paramsJson == null ? null : paramsJson.getJSONObject("properties");
        if (props == null) return;

        JSONObject confirmed = new JSONObject(true);
        confirmed.put("type", "boolean");
        confirmed.put("description", "是否已获得用户明确确认。默认不带此参数或设为 false，工具返回改动清单（含 draftId）供你转述给用户；"
                + "用户明确同意后，用返回结果中的 draftId 加 confirmed=true 重新调用即可执行，无需重复传入其他内容参数。"
                + "仅当用户已看到包含本次改动具体内容的方案或清单（如具体字段、页面结构、记录内容）并明确回复确认/保存/执行时，"
                + "才可在首次调用时直接携带 confirmed=true 立即执行，并在最终回复中附上改动要点供用户事后核对。"
                + "「按你自己的想法做」「你看着办」等概括性授权不构成确认，仍须先转述改动清单征得同意");
        props.put("confirmed", confirmed);

        JSONObject draftId = new JSONObject(true);
        draftId.put("type", "string");
        draftId.put("description", "确认草稿 ID。工具返回 needConfirm 时会附带 draftId；用户确认后重新调用时只需传本参数与 "
                + "confirmed=true，系统会自动恢复此前提交的内容，无需重复生成");
        props.put("draftId", draftId);
    }

    /**
     * 确认调用（confirmed 且带 draftId）时凭缓存草稿恢复完整参数，避免模型重复生成大字段；草稿过期则抛异常。
     *
     * @param toolName
     * @param user
     * @param arguments
     * @return
     */
    static String prepareArgs(String toolName, ID user, String arguments) {
        JSONObject args = ToolHelper.parseObject(arguments);
        if (args == null || !args.getBooleanValue("confirmed") || StringUtils.isBlank(args.getString("draftId"))) {
            return arguments;
        }

        String cached = Application.getCommonsCache().get(draftKey(toolName, user, args.getString("draftId")));
        if (StringUtils.isBlank(cached)) {
            throw new KnownToolException("确认草稿已过期或不存在，请重新生成内容后再提交确认");
        }

        JSONObject base = ToolHelper.parseObject(cached);
        if (base == null) base = new JSONObject();
        base.putAll(args);  // 本次显式字段优先，允许确认时微调
        base.put("confirmed", true);
        base.remove("draftId");
        return base.toJSONString();
    }

    /**
     * needConfirm 结果注入 draftId 并缓存参数；成功执行则清除已消费草稿（失败会抛异常，草稿留待重试至 TTL）。
     *
     * @param toolName
     * @param user
     * @param arguments
     * @param execArgs
     * @param toolRes
     * @return
     */
    static String handleResult(String toolName, ID user, String arguments, String execArgs, String toolRes) {
        JSONObject result = ToolHelper.parseObject(toolRes);
        if (result == null) return toolRes;

        if (result.getBooleanValue("needConfirm")) {
            String draftId = CommonsUtils.randomHex(true);
            Application.getCommonsCache().put(draftKey(toolName, user, draftId), execArgs, DRAFT_TTL);
            result.put("draftId", draftId);
            result.put("message", "本次操作尚未执行。请将上述改动清单完整转述给用户并征求确认，"
                    + "用户明确同意后以 {\"draftId\":\"" + draftId + "\", \"confirmed\":true} 重新调用本工具执行，"
                    + "无需重复传入其他参数（系统已缓存本次内容）。用户明确拒绝或要求调整时不得执行。");
            return result.toJSONString();
        }

        if ("ok".equals(result.getString("status"))) {
            JSONObject args = ToolHelper.parseObject(arguments);
            if (args != null && args.getBooleanValue("confirmed") && StringUtils.isNotBlank(args.getString("draftId"))) {
                Application.getCommonsCache().evict(draftKey(toolName, user, args.getString("draftId")));
            }
        }
        return toolRes;
    }

    /**
     * 草稿缓存 key，按工具与用户隔离。
     *
     * @param toolName
     * @param user
     * @param draftId
     * @return
     */
    static String draftKey(String toolName, ID user, String draftId) {
        return DRAFT_KEY_PREFIX + toolName + ":" + user + ":" + draftId;
    }
}
