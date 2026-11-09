/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartImage;
import com.openai.models.chat.completions.ChatCompletionContentPartText;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.service.AibotChatService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.rebuild.core.metadata.EntityHelper.AibotChat;

/**
 * @author Zixin
 * @since 2025/11/1
 */
@Slf4j
public abstract class ChatManager {

    // 会话来源：公开访问（含匿名访客）
    public static final String SOURCE_PUB = "PUB";

    /**
     * @param user
     * @return
     */
    public static ID initChat(ID user, String subject) {
        return initChat(user, subject, null, null);
    }

    /**
     * @param user
     * @param subject
     * @param agentId 使用的智能体（空为默认）
     * @return
     */
    public static ID initChat(ID user, String subject, ID agentId) {
        return initChat(user, subject, agentId, null);
    }

    /**
     * @param user
     * @param subject
     * @param agentId 使用的智能体（空为默认）
     * @param source 会话来源（空为用户会话，非空为系统调用/公开访问）
     * @return
     */
    public static ID initChat(ID user, String subject, ID agentId, String source) {
        Record chat = EntityHelper.forNew(AibotChat, user);

        if (StringUtils.isBlank(subject)) subject = "新会话";
        else subject = CommonsUtils.maxstr(subject, 40);
        chat.setString("subject", subject);
        if (agentId != null) chat.setID("agentId", agentId);
        if (StringUtils.isNotBlank(source)) chat.setString("source", source);

        chat = Application.getCommonsService().createOrUpdate(chat);
        return chat.getPrimary();
    }

    /**
     * @param chatid
     * @return
     */
    public static Chat getChat(ID chatid) {
        String ckey = "chat2-" + chatid;
        Chat chat = (Chat) Application.getCommonsCache().getx(ckey);
        if (chat == null) {
            chat = new Chat(chatid);
            Application.getCommonsCache().putx(ckey, chat);
        } else {
            ID agentId = chat.getAgent().getAgentId();
            if (agentId != null) {
                AibotAgent agent = AgentDefs.getAgentQuietly(agentId);
                chat.refreshAgent(agent != null ? agent : AibotAgent.defaultAgent());
            }
        }
        return chat;
    }

    /**
     * 指定 Agent 的会话
     *
     * @param chatid
     * @param agent
     * @return
     */
    public static Chat getChat(ID chatid, AibotAgent agent) {
        String ckey = "chat2-" + chatid;
        Chat chat = (Chat) Application.getCommonsCache().getx(ckey);
        if (chat == null) {
            chat = new Chat(chatid, agent);
            Application.getCommonsCache().putx(ckey, chat);
        } else {
            chat.refreshAgent(agent);
        }
        return chat;
    }

    /**
     * @param chat
     */
    public static void storeChat(Chat chat) {
        String ckey = "chat2-" + chat.getChatid();
        Application.getCommonsCache().putx(ckey, chat);

        ID user = ObjectUtils.getIfNull(UserContextHolder.getUser(true), UserService.SYSTEM_USER);
        Record r = EntityHelper.forUpdate(chat.getChatid(), user);

        JSONArray contents = new JSONArray();
        chat.getMessages().forEach(m -> contents.add(m.toJSON()));

        String contents2s = contents.toJSONString();
        r.setString("contents", contents2s);

        // 归属 Agent（默认的不落库，即空为默认）
        if (chat.getAgent().getAgentId() != null) r.setID("agentId", chat.getAgent().getAgentId());

        long tokenUsage = chat.getTokenUsage();
        r.setLong("token", tokenUsage > 0 ? tokenUsage : contents2s.length());
        Application.getCommonsService().createOrUpdate(r);
    }

    /**
     * 删除会话（支持回收站）
     *
     * @param chatid
     * @see AibotChatService#delete(ID)
     */
    public static void deleteChat(ID chatid) {
        Application.getBean(AibotChatService.class).delete(chatid);
    }

    /**
     * 直接提问/回答（程序调用统一入口）
     * user 为空时以 Rex 身份提问，会话归属 Rex；指定 user 时以该用户身份执行，会话归属该用户
     *
     * @param params
     * @return [0]=AI 回答，[1]=chatid 字面量
     */
    public static String[] ask(AskRequest params) {
        if (params.getUser() == null) {
            return doAsk(params, UserService.AIBOT_USER);
        }

        ID keepCurrentUser = UserContextHolder.setUser(params.getUser());
        try {
            return doAsk(params, params.getUser());
        } finally {
            UserContextHolder.clearUser(keepCurrentUser);
        }
    }

    private static String[] doAsk(AskRequest params, ID user) {
        ID chatid = params.getChatid();
        boolean newChat = chatid == null;

        if (newChat) {
            // 超限校验在会话落库前，避免超限后残留仅有主题的空会话
            String quotaExceeded = AgentQuota.check(params.getAgent());
            if (quotaExceeded != null) throw new AiBotException(quotaExceeded);

            chatid = initChat(user, params.getUserContent(),
                    params.getAgent() == null ? null : params.getAgent().getAgentId(), params.getSource());
        }

        Chat chat;
        if (newChat && params.getAgent() != null) {
            appendPrompt(params.getAgent(), params.getPrompt());
            chat = new Chat(chatid, params.getAgent());
        } else if (newChat) {
            chat = new Chat(chatid, null, params.getPrompt());
        } else {
            // 会话已绑定的智能体优先，忽略本次 agent
            chat = new Chat(chatid);

            // 已有会话在其绑定智能体上校验（新建场景已在落库前校验）
            String quotaExceeded = AgentQuota.check(chat.getAgent());
            if (quotaExceeded != null) throw new AiBotException(quotaExceeded);

            appendPrompt(chat.getAgent(), params.getPrompt());
        }

        AibotAgent prevAgent = AibotContextHolder.swapAgent(chat.getAgent());
        try {
            String result;
            try {
                List<File> files = params.getFiles();
                if (CollectionUtils.isEmpty(files)) {
                    result = chat.ask(params.getUserContent());
                } else {
                    // 通过 AI 视觉能力识别图片内容并返回文本描述（支持多张图片）
                    List<ChatCompletionContentPart> parts = new ArrayList<>();
                    parts.add(ChatCompletionContentPart.ofText(
                            ChatCompletionContentPartText.builder().text(params.getUserContent()).build()));

                    for (File imageFile : files) {
                        String base64 = CommonsUtils.fileToBase64(imageFile);
                        String mimeType;
                        try {
                            mimeType = Config.TIKA.detect(imageFile);
                        } catch (IOException e) {
                            mimeType = "image/png";
                            log.warn("Failed to detect image mime type, fallback to png : {}", imageFile.getName(), e);
                        }

                        String dataUrl = String.format("data:%s;base64,%s", mimeType, base64);
                        parts.add(ChatCompletionContentPart.ofImageUrl(
                                ChatCompletionContentPartImage.builder()
                                        .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder().url(dataUrl).build())
                                        .build()));
                    }

                    result = chat.ask(params.getUserContent(), parts);
                }
            } catch (Exception ex) {
                // 失败时用户消息也落库，避免产生空会话记录（落库失败不掩盖原始异常）
                try {
                    chat.store();
                } catch (Exception storeEx) {
                    log.error("Cannot store chat on failure : {}", chatid, storeEx);
                }
                throw ex;
            }

            // 补充 AI 消息后落库
            chat.completionAfter(result, null, null);
            return new String[]{result, chatid.toLiteral()};
        } finally {
            AibotContextHolder.restoreAgent(prevAgent);
        }
    }

    /**
     * @param agent
     * @param prompt
     */
    private static void appendPrompt(AibotAgent agent, String prompt) {
        if (agent == null || StringUtils.isBlank(prompt)) return;
        String base = agent.getPrompt();
        agent.setPrompt(StringUtils.isBlank(base) ? prompt : base + "\n\n" + prompt);
    }
}
