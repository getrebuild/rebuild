/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.aibot;

import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.service.aibot.ChatClient;
import com.rebuild.core.service.aibot.Config;
import com.rebuild.core.service.aibot.Message;
import com.rebuild.core.service.aibot.MessageCompletions;
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

    @PostMapping("chat")
    public void chat(HttpServletRequest req, HttpServletResponse resp) {
        RequestBody requestBody = new RequestBody(req);
        Message respMessage = ChatClient.instance.post(requestBody.getChatid(), requestBody.getUserContent());
        ServletUtils.writeJson(resp, respMessage.toDeepChat(false).toJSONString());
    }

    @PostMapping("chat-stream")
    public void chatStream(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RequestBody requestBody = new RequestBody(req);
        ChatClient.instance.stream(requestBody.getChatid(), requestBody.getUserContent(), resp);
    }

    @GetMapping("chat-init")
    public RespBody chatInit(HttpServletRequest req) {
        String chatid = req.getHeader("chatid");
        MessageCompletions completions;
        if (chatid != null) {
            completions = (MessageCompletions) Application.getCommonsCache().getx(chatid);
        } else {
            completions = ChatClient.instance.createMessageCompletions(Config.getBasePrompt());
        }

        JSONArray history = new JSONArray();
        for (Message m : completions.getMessages()) {
            history.add(m.toDeepChat(true));
        }

        return RespBody.ok(JSONUtils.toJSONObject(
                new String[]{"chatid", "history"}, new Object[]{completions.getId(), history}));
    }

    @GetMapping("chat-list")
    public RespBody chatList(HttpServletRequest req, HttpServletResponse resp) {
        String dsSecret = RebuildConfiguration.get(ConfigurationItem.AibotDSSecret);
        if (dsSecret == null) {
            return RespBody.error(Language.L("请配置后使用"));
        }
        return RespBody.ok();
    }

    @GetMapping("chat-delete")
    public RespBody chatDelete(HttpServletRequest req, HttpServletResponse resp) {
        return null;
    }
}
