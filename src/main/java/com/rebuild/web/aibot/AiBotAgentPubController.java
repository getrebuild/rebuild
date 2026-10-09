/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.aibot;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.AgentDefs;
import com.rebuild.core.aibot2.AibotAgent;
import com.rebuild.core.aibot2.AibotContextHolder;
import com.rebuild.core.aibot2.Chat;
import com.rebuild.core.aibot2.ChatManager;
import com.rebuild.core.aibot2.ChatRequest;
import com.rebuild.core.aibot2.StreamEcho;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.License;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.commons.RbvMissingController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Agent 对外发布（免登录）
 *
 * @author Zixin
 * @since 4.1
 */
@Slf4j
@RestController
@RequestMapping("/aibot/pub")
public class AiBotAgentPubController extends BaseController {

    @GetMapping("{agentId}")
    public ModelAndView pubIndex(@PathVariable String agentId, HttpServletResponse response) throws IOException {
        if (!License.isCommercial()) {
            return RbvMissingController.errorUnsupported(Language.L("专属智能体"));
        }

        ID id = getAvailableAgent(agentId);
        if (id == null) {
            response.sendError(404);
            return null;
        }

        ModelAndView mv = createModelAndView("/aibot/chat-pub");
        mv.getModelMap().put("agentId", agentId);
        mv.getModelMap().put("agentName", AgentDefs.getAgent(id).getName());
        mv.getModelMap().put("pageFooter", Language.L("由 REBUILD AI 助手强力驱动"));
        return mv;
    }

    @GetMapping("{agentId}/chat-init")
    public RespBody chatInit(@PathVariable String agentId, HttpServletRequest req) {
        ID id = getAvailableAgent(agentId);
        if (id == null) return RespBody.error(404);

        ID chatid = getOwnedChatId(req, id);

        JSONArray messages = new JSONArray();
        JSONArray suggestQuestions = null;
        if (chatid != null) {
            Chat chat = ChatManager.getChat(chatid, AgentDefs.getAgent(id));
            chat.getMessages().forEach(m -> messages.add(m.toJSON()));
        } else {
            JSONObject publish = AgentDefs.getPublishConfig(id);
            String welcome = publish.getString("welcome");
            if (StringUtils.isBlank(welcome)) {
                welcome = String.format("欢迎使用 %s！有什么问题都可以向我提问哦", AgentDefs.getAgent(id).getName());
            }

            JSON welcomeMsg = JSONUtils.toJSONObject(
                    new String[]{"role", "content"},
                    new Object[]{"ai", welcome});
            messages.add(welcomeMsg);

            // 配置的引导问题（每行一个）
            String suggests = publish.getString("suggestQuestions");
            if (StringUtils.isNotBlank(suggests)) {
                suggestQuestions = new JSONArray();
                for (String line : suggests.split("\n")) {
                    String q = line.trim();
                    if (!q.isEmpty()) suggestQuestions.add(q);
                }
                if (suggestQuestions.isEmpty()) suggestQuestions = null;
            }
        }

        JSONObject data = JSONUtils.toJSONObject(
                new String[]{"_chatid", "messages"}, new Object[]{chatid, messages});
        if (suggestQuestions != null) data.put("suggestQuestions", suggestQuestions);
        return RespBody.ok(data);
    }

    @PostMapping("{agentId}/chat-stream")
    public void chatStream(@PathVariable String agentId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ID id = getAvailableAgent(agentId);
        if (id == null) {
            StreamEcho.error(Language.L("Agent 不存在或未发布"), resp.getWriter());
            return;
        }
        AibotAgent agent = AgentDefs.getAgent(id);
        if (!agent.available()) {
            StreamEcho.error(Language.L("请联系管理员配置 AI 助手后使用"), resp.getWriter());
            return;
        }

        // 权限代理：公开访问必须绑定有效用户，不回退系统用户（权限过大）
        ID bindUser = AgentDefs.getBindUser(id);
        if (bindUser == null) {
            StreamEcho.error(Language.L("请联系管理员为该 Agent 绑定用户后使用"), resp.getWriter());
            return;
        }

        JSONObject reqJson = (JSONObject) ServletUtils.getRequestJson(req);
        reqJson.remove("skill");
        reqJson.remove("planMode");
        reqJson.remove("planConfirmed");

        ID chatid = getOwnedChatId(req, id);

        ID keepUser = UserContextHolder.setUser(bindUser);
        AibotContextHolder.setAgent(agent, true);

        try {
            if (chatid == null) {
                chatid = ChatManager.initChat(UserService.AIBOT_USER,
                        "PUB:" + agent.getName() + ":" + StringUtils.trimToEmpty(reqJson.getString("content")), id);
            }

            ChatRequest chatRequest = new ChatRequest(reqJson, chatid);
            Chat chat = ChatManager.getChat(chatid, agent);

            if (!chat.tryBeginRun()) {
                StreamEcho.error(Language.L("会话正在处理中，请稍后再试"), resp.getWriter());
                return;
            }

            try {
                chat.stream(chatRequest, resp);
            } catch (Throwable ex) {
                log.error("pub-chat-stream", ex);
                String errorMsg = Language.L("请求错误") + ":" + CommonsUtils.getRootMessage(ex);
                try {
                    StreamEcho.error(errorMsg, resp.getWriter());
                } catch (Exception ignored) {
                    // writer 可能已关闭
                }

                // 错误落库
                try {
                    chat.completionError(errorMsg, chatRequest);
                } catch (Exception e) {
                    log.warn("Failed to save error message for chat", e);
                }
            } finally {
                chat.endRun();
            }
        } finally {
            AibotContextHolder.clear();
            UserContextHolder.clearUser(keepUser);
        }
    }

    private ID getOwnedChatId(HttpServletRequest req, ID agentId) {
        ID chatid = getIdParameter(req, "chatid");
        if (chatid == null) return null;

        Object[] e = Application.createQueryNoFilter(
                "select agentId,createdBy from AibotChat where chatId = ?")
                .setParameter(1, chatid)
                .unique();
        if (e == null) return null;

        if (!agentId.equals(e[0])) return null;
        if (!UserService.AIBOT_USER.equals(e[1])) return null;
        return chatid;
    }

    /**
     * 校验 Agent 可用（存在、未禁用、已启用发布）
     *
     * @param agentId
     * @return 合法返回 ID，否则 null
     */
    private ID getAvailableAgent(String agentId) {
        if (!ID.isId(agentId)) return null;

        ID id = ID.valueOf(agentId);
        if (AgentDefs.getAgent(id) == null) return null;
        if (!AgentDefs.getPublishConfig(id).getBooleanValue("enabled")) return null;
        return id;
    }
}
