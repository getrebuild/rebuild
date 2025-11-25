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
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.aibot.StreamEcho;
import com.rebuild.core.service.aibot2.Chat;
import com.rebuild.core.service.aibot2.ChatManager;
import com.rebuild.core.service.aibot2.ChatRequest;
import com.rebuild.core.service.aibot2.Message;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
        Chat chat = initChat(req);
        Message respMessage = chat.post(new ChatRequest(req, chat.getChatid()));
        ServletUtils.writeJson(resp, respMessage.toJSON().toJSONString());
    }

    @PostMapping("post/chat-stream")
    public void chatStream(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (RebuildConfiguration.get(ConfigurationItem.AibotDSSecret) == null) {
            StreamEcho.error("请配置 AI 助手参数后继续", resp.getWriter());
            return;
        }

        Chat chat = initChat(req);
        try {
            chat.stream(new ChatRequest(req, chat.getChatid()), resp);
        } catch (Exception ex) {
            log.error("chat-stream", ex);
            StreamEcho.error("请求错误:" + CommonsUtils.getRootMessage(ex), resp.getWriter());
        }
    }

    private Chat initChat(HttpServletRequest req) {
        ID chatid = getIdParameter(req, "chatid");
        if (chatid == null) {
            chatid = ChatManager.initChat(getRequestUser(req));
        }
        return ChatManager.getChat(chatid);
    }

    @GetMapping("post/chat-init")
    public RespBody chatInit(HttpServletRequest req) {
        ID chatid = getIdParameter(req, "chatid");

        JSONArray messages = new JSONArray();
        if (chatid != null) {
            Chat chat = ChatManager.getChat(chatid);
            chat.getMessages().forEach(m -> messages.add(m.toJSON()));
        } else {
            JSON welcome = JSONUtils.toJSONObject(
                    new String[]{"role", "content"},
                    new Object[]{"ai", "欢迎使用 REBUILD AI 助手！有什么问题都可以向我提问哦"});
            messages.add(welcome);
        }

        return RespBody.ok(JSONUtils.toJSONObject(
                new String[]{"_chatid", "messages"}, new Object[]{chatid, messages}));
    }

    @PostMapping("post/chat-delete")
    public RespBody chatDelete(HttpServletRequest req) {
        ChatManager.deleteChat(getIdParameterNotNull(req, "chatid"));
        return RespBody.ok();
    }

    @GetMapping("post/chat-list")
    public RespBody chatList(HttpServletRequest req) {
        Object[][] chats = Application.createQueryNoFilter(
                "select chatId,subject,createdOn from AibotChat where createdBy = ? order by modifiedOn desc")
                .setParameter(1, getRequestUser(req))
                .array();

        return RespBody.ok(JSONUtils.toJSONObjectArray(
                new String[]{"chatid", "subject", "createdOn"}, chats));
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
}
