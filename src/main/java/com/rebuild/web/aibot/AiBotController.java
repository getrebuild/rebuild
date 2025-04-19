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
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.service.aibot.ChatClient;
import com.rebuild.core.service.aibot.ChatRequest;
import com.rebuild.core.service.aibot.ChatStore;
import com.rebuild.core.service.aibot.Message;
import com.rebuild.core.service.aibot.MessageCompletions;
import com.rebuild.core.service.aibot.StreamEcho;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao
 * @since 2025/4/12
 */
@Slf4j
@RestController
@RequestMapping("/aibot")
public class AiBotController extends BaseController {

    @PostMapping("post/chat")
    public void chat(HttpServletRequest req, HttpServletResponse resp) {
        Message respMessage = ChatClient.instance.post(new ChatRequest(req));
        ServletUtils.writeJson(resp, respMessage.toClientJSON().toJSONString());
    }

    @PostMapping("post/chat-stream")
    public void chatStream(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (RebuildConfiguration.get(ConfigurationItem.AibotDSSecret) == null) {
            StreamEcho.error("请配置 AI 助手参数后继续", resp.getWriter());
            return;
        }

        try {
            ChatClient.instance.stream(new ChatRequest(req), resp);
        } catch (Exception ex) {
            StreamEcho.error("请求错误:" + ex.getLocalizedMessage(), resp.getWriter());
        }
    }

    @GetMapping("post/chat-init")
    public RespBody chatInit(HttpServletRequest req) {
        final ID chatid = getIdParameter(req, "chatid");
        JSONArray messages = new JSONArray();
        if (chatid != null) {
            MessageCompletions c = ChatStore.instance.get(chatid);
            if (c != null) {
                c.getMessages().forEach(m -> messages.add(m.toClientJSON()));
            }
        } else {
            JSON welcome = JSONUtils.toJSONObject(
                    new String[]{"role", "content"},
                    new Object[]{"ai", "欢迎使用 REBUILD AI 助手！有什么问题都可以向我提问哦"});
            messages.add(welcome);
        }

        return RespBody.ok(JSONUtils.toJSONObject(
                new String[]{"_chatid", "messages"}, new Object[]{chatid, messages}));
    }

    @GetMapping("post/chat-list")
    public RespBody chatList(HttpServletRequest req) {
        Object[][] chats = Application.createQueryNoFilter(
                "select chatId,subject,createdOn from AibotChat where createdBy = ? order by createdOn desc")
                .setParameter(1, getRequestUser(req))
                .array();
        JSON res = JSONUtils.toJSONObjectArray(new String[]{"chatid", "subject", "createdOn"}, chats);
        return RespBody.ok(res);
    }

    @PostMapping("post/chat-delete")
    public RespBody chatDelete(HttpServletRequest req) {
        ChatStore.instance.delete(getIdParameterNotNull(req, "chatid"));
        return RespBody.ok();
    }

    @PostMapping("post/chat-reanme")
    public RespBody chatRename(HttpServletRequest req) {
        // TODO
        return RespBody.ok();
    }

    @GetMapping("chat")
    public ModelAndView chatIndex() {
        ModelAndView mv = createModelAndView("/aibot/chat-view");
        mv.getModelMap().put("pageFooter", Language.L("由 REBUILD AI 助手强力驱动"));
        return mv;
    }

    @GetMapping("redirect")
    public void chatRedirect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = req.getParameter("id");
        resp.sendRedirect("../");
    }
}
