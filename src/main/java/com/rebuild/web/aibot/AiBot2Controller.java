/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.aibot;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.aibot2.Chat;
import com.rebuild.core.aibot2.ChatLogger;
import com.rebuild.core.aibot2.ChatManager;
import com.rebuild.core.aibot2.ChatRequest;
import com.rebuild.core.aibot2.Config;
import com.rebuild.core.aibot2.Message;
import com.rebuild.core.aibot2.SkillDefs;
import com.rebuild.core.aibot2.StreamEcho;
import com.rebuild.core.aibot2.SuggestQuestions;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.SysbaseSupport;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author devezhao
 * @since 2025/4/12
 */
@Slf4j
@RestController
@RequestMapping("/aibot2")
public class AiBot2Controller extends BaseController {

    @PostMapping("post/chat")
    public void chat(HttpServletRequest req, HttpServletResponse resp) {
        ChatRequest chatRequest = buildChatRequest(req);
        Chat chat = ChatManager.getChat(chatRequest.getChatid());

        if (!chat.tryBeginRun()) {
            JSONObject error = JSONUtils.toJSONObject("error", Language.L("会话正在处理中，请稍后再试"));
            ServletUtils.writeJson(resp, error.toJSONString());
            return;
        }

        try {
            Message respMessage = chat.post(chatRequest);
            ServletUtils.writeJson(resp, respMessage.toJSON().toJSONString());
        } catch (Throwable ex) {
            log.error("chat-post", ex);
            String errorMsg = "请求错误:" + CommonsUtils.getRootMessage(ex);
            JSONObject error = JSONUtils.toJSONObject("error", errorMsg);
            ServletUtils.writeJson(resp, error.toJSONString());

            // 错误落库
            try {
                chat.completionAfter(errorMsg, null, chatRequest);
            } catch (Exception e) {
                log.warn("Failed to save error message for chat", e);
            }
        } finally {
            chat.endRun();
        }
    }

    @PostMapping("post/chat-stream")
    public void chatStream(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!Config.availableAiBot()) {
            StreamEcho.error(Language.L("请联系管理员配置 AI 助手后使用"), resp.getWriter());
            return;
        }

        ChatRequest chatRequest = buildChatRequest(req);
        Chat chat = ChatManager.getChat(chatRequest.getChatid());

        if (!chat.tryBeginRun()) {
            StreamEcho.error(Language.L("会话正在处理中，请稍后再试"), resp.getWriter());
            return;
        }

        try {
            chat.stream(chatRequest, resp);
        } catch (Throwable ex) {
            log.error("chat-stream", ex);
            String errorMsg = "请求错误:" + CommonsUtils.getRootMessage(ex);
            try {
                StreamEcho.error(errorMsg, resp.getWriter());
            } catch (Exception ignored) {
                // writer 可能已关闭
            }

            // 错误落库
            try {
                chat.completionAfter(errorMsg, null, chatRequest);
            } catch (Exception e) {
                log.warn("Failed to save error message for chat", e);
            }
        } finally {
            chat.endRun();
        }
    }

    private ChatRequest buildChatRequest(HttpServletRequest req) {
        JSONObject reqJson = (JSONObject) ServletUtils.getRequestJson(req);
        ID chatid = getIdParameter(req, "chatid");
        if (chatid == null) {
            String s = reqJson.getString("content");
            chatid = ChatManager.initChat(getRequestUser(req), s);
        }

        return new ChatRequest(reqJson, chatid);
    }

    @PostMapping("post/chat-stream-stop")
    public RespBody chatStreamStop(HttpServletRequest req) {
        ID chatid = getIdParameterNotNull(req, "chatid");
        StreamEcho.setInterrupt(chatid);
        return RespBody.ok();
    }

    @GetMapping("post/chat-init")
    public RespBody chatInit(HttpServletRequest req) {
        ID chatid = getIdParameter(req, "chatid");

        JSONArray messages = new JSONArray();
        JSONArray suggestQuestions = null;
        if (chatid != null) {
            Chat chat = ChatManager.getChat(chatid);
            chat.getMessages().forEach(m -> messages.add(m.toJSON()));
        } else {
            String welcome = RebuildConfiguration.get(ConfigurationItem.AibotWelcome);
            if (StringUtils.isBlank(welcome)) {
                String aibotName = RebuildConfiguration.get(ConfigurationItem.AibotName);
                welcome = String.format("欢迎使用 %s！有什么问题都可以向我提问哦", aibotName);
            }

            JSON welcomeMsg = JSONUtils.toJSONObject(
                    new String[]{"role", "content"},
                    new Object[]{"ai", welcome});
            messages.add(welcomeMsg);

            try {
                suggestQuestions = SuggestQuestions.generate(getRequestUser(req));
            } catch (Exception ex) {
                log.warn("SuggestQuestions failed", ex);
            }
        }

        JSONObject data = JSONUtils.toJSONObject(
                new String[]{"_chatid", "messages"}, new Object[]{chatid, messages});
        if (suggestQuestions != null) {
            data.put("suggestQuestions", suggestQuestions);
        }
        return RespBody.ok(data);
    }

    @GetMapping("skills")
    public RespBody skills() {
        return RespBody.ok(SkillDefs.listSkills());
    }

    @PostMapping("post/chat-delete")
    public RespBody chatDelete(HttpServletRequest req) {
        ChatManager.deleteChat(getIdParameterNotNull(req, "chatid"));
        return RespBody.ok();
    }

    @GetMapping("post/chat-list")
    public RespBody chatList(HttpServletRequest req) {
        Object[][] chats = Application.createQueryNoFilter(
                "select chatId,subject,modifiedOn from AibotChat where createdBy = ? order by modifiedOn desc")
                .setParameter(1, getRequestUser(req))
                .array();

        // 按日期分组：今天 / 最近一周 / 更早
        long todayStart = org.apache.commons.lang3.time.DateUtils
                .truncate(new Date(), Calendar.DAY_OF_MONTH).getTime();
        long weekStart = todayStart - 7 * 24 * 60 * 60 * 1000L;

        List<Object[]> today = new ArrayList<>();
        List<Object[]> week = new ArrayList<>();
        List<Object[]> earlier = new ArrayList<>();
        for (Object[] c : chats) {
            long t = ((Date) c[2]).getTime();
            if (t >= todayStart) today.add(c);
            else if (t >= weekStart) week.add(c);
            else earlier.add(c);
        }

        JSONArray data = new JSONArray();
        addChatGroup(data, "today", today);
        addChatGroup(data, "week", week);
        addChatGroup(data, "earlier", earlier);
        return RespBody.ok(data);
    }

    private void addChatGroup(JSONArray data, String group, List<Object[]> items) {
        if (items.isEmpty()) return;

        data.add(JSONUtils.toJSONObject(
                new String[]{"group", "items"},
                new Object[]{group, JSONUtils.toJSONObjectArray(
                        new String[]{"chatid", "subject", "modifiedOn"},
                        items.toArray(new Object[0][]))}));
    }

    @PostMapping("post/chat-rename")
    public RespBody chatRename(HttpServletRequest req) {
        ID chatid = getIdParameterNotNull(req, "chatid");
        String subject = getParameterNotNull(req, "s");

        Record r = EntityHelper.forUpdate(chatid, getRequestUser(req));
        r.setString("subject", subject);
        Application.getCommonsService().update(r);

        return RespBody.ok();
    }

    @PostMapping("post/chat-feedback")
    public RespBody chatFeedback(HttpServletRequest req) {
        ID chatid = getIdParameterNotNull(req, "chatid");
        String type = getParameterNotNull(req, "type");
        String comment = getParameter(req, "comment");

        ChatLogger chatLogger = ChatManager.getChat(chatid).chatLogger();
        File logFile = chatLogger.getLogFile();
        if (!logFile.exists()) return RespBody.error();

        String feedbackDetail = StringUtils.isBlank(comment) ? type : type + " : " + comment;
        chatLogger.log("FEEDBACK", feedbackDetail);

        TaskExecutors.queue(() -> new SysbaseSupport().uploadAibotFeedback(logFile, comment));
        return RespBody.ok();
    }
}
