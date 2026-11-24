/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartImage;
import com.openai.models.chat.completions.ChatCompletionContentPartText;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.service.AibotChatService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.rebuild.core.aibot2.vector.VectorData.NN;
import static com.rebuild.core.metadata.EntityHelper.AibotChat;

/**
 * @author Zixin
 * @since 2025/11/1
 */
@Slf4j
public abstract class ChatManager {

    // 会话来源：空为用户会话，SYS: 前缀为系统调用，其余为渠道来源（PUB 公开访问、企微等）
    public static final String SOURCE_PUB = "PUB";
    public static final String SOURCE_SYS = "SYS:";

    // 可见的会话：真实用户会话与公开访问会话，排除系统用户及 SYS: 前缀的系统调用（定时任务、内部集成等）
    public static final String VISIBLE_WHERE = "createdBy <> '" + UserService.SYSTEM_USER + "'"
            + " and (createdBy <> '" + UserService.AIBOT_USER + "' or source = '" + SOURCE_PUB + "')"
            + " and (source is null or source not like '" + SOURCE_SYS + "%')";

    // 摘要输入上限（字符），只取最近内容避免超出模型上下文
    private static final int SUMMARY_MAX_CHARS = 12000;

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
     * @param source 会话来源（空为用户会话；SYS: 前缀为系统调用；其他为渠道来源，如 PUB 公开访问、企微）
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
     * 会话是否正在回答（仅查缓存，不加载会话）
     *
     * @param chatid
     * @return
     */
    public static boolean isChatRunning(ID chatid) {
        Chat chat = (Chat) Application.getCommonsCache().getx("chat2-" + chatid);
        return chat != null && chat.isRunning();
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
     * 用户按需取：指定用户 > 当前线程用户，会话归属该用户；均无时以 Rex 身份提问，会话归属 Rex
     *
     * @param params
     * @return [0]=AI 回答，[1]=chatid 字面量
     */
    public static String[] ask(AskRequest params) {
        ID user = ObjectUtils.getIfNull(params.getUser(), UserContextHolder.getUser(true));
        if (user == null) {
            return doAsk(params, UserService.AIBOT_USER);
        }

        ID keepCurrentUser = UserContextHolder.setUser(user);
        try {
            return doAsk(params, user);
        } finally {
            UserContextHolder.clearUser(keepCurrentUser);
        }
    }

    private static String[] doAsk(AskRequest params, ID user) {
        ID chatid = params.getChatid();
        boolean newChat = chatid == null;

        if (newChat) {
            String modelConfigError = getModelConfigError(params.getAgent());
            if (modelConfigError != null) throw new AiBotException(modelConfigError);

            // 超限校验在会话落库前，避免超限后残留仅有主题的空会话
            String quotaExceeded = AgentQuota.check(params.getAgent());
            if (quotaExceeded != null) throw new AiBotException(quotaExceeded);

            chatid = initChat(user, params.getUserContent(),
                    params.getAgent() == null ? null : params.getAgent().getAgentId(), params.getSource());
        }

        Chat chat;
        if (newChat && params.getAgent() != null) {
            chat = new Chat(chatid, params.getAgent());
            chat.appendPrompt(params.getPrompt());
        } else if (newChat) {
            chat = new Chat(chatid, null, params.getPrompt());
        } else {
            // 会话已绑定的智能体优先，忽略本次 agent
            chat = getChat(chatid);

            String modelConfigError = getModelConfigError(chat.getAgent());
            if (modelConfigError != null) throw new AiBotException(modelConfigError);

            // 已有会话在其绑定智能体上校验（新建场景已在落库前校验）
            String quotaExceeded = AgentQuota.check(chat.getAgent());
            if (quotaExceeded != null) throw new AiBotException(quotaExceeded);

            chat.appendPrompt(params.getPrompt());
        }

        // 新会话立即入缓存（已有会话经 getChat 已入），使同一会话的并发提问（如企微连续消息）拿到同一实例，经 tryBeginRun 串行
        if (newChat) Application.getCommonsCache().putx("chat2-" + chatid, chat);
        if (!chat.tryBeginRun()) throw new AiBotException(Language.L("会话正在处理中，请稍后再试"));

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
                // 失败时落库用户消息与错误消息，避免历史中出现无回复的提问（落库失败不掩盖原始异常）
                try {
                    chat.completionError(Language.L("请求错误") + ":" + CommonsUtils.getRootMessage(ex), null);
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
            chat.endRun();
        }
    }

    // 智能体引用的模型配置不可用（已停用或已删除）
    private static String getModelConfigError(AibotAgent agent) {
        return agent == null ? null : StringUtils.trimToNull(agent.getModelConfigError());
    }

    /**
     * 压缩会话为简短概要（引用会话注入用）
     *
     * @param chatid
     * @return
     */
    public static String summarizeChat(ID chatid) {
        JSONObject summary = buildChatSummary(chatid, false);
        return summary == null ? null : summary.getString("short");
    }

    /**
     * 压缩会话为两版摘要（后台整理调用，不经聊天请求链路，不产生隐藏会话，也不计入用户配额）
     *
     * @param chatid
     * @param reload 从数据库重新加载会话（后台任务用，避免使用可能过期的缓存）
     * @return {"short": 简短概要, "long": 详细摘要}，生成失败返回 null
     */
    public static JSONObject buildChatSummary(ID chatid, boolean reload) {
        Chat chat = reload ? new Chat(chatid) : getChat(chatid);
        final int msgCount = chat.getMessages().size();

        // 已有摘要且之后没有新消息，直接复用（后台任务生成）
        if (!reload) {
            Object[] stored = Application.createQueryNoFilter(
                    "select summary from AibotChat where chatId = ?")
                    .setParameter(1, chatid)
                    .unique();
            if (stored != null && stored[0] != null) {
                JSONObject summary = JSON.parseObject((String) stored[0]);
                if (summary != null && summary.getIntValue("messages") == msgCount
                        && StringUtils.isNotBlank(summary.getString("short"))
                        && StringUtils.isNotBlank(summary.getString("long"))) {
                    return summary;
                }
            }
        }

        StringBuilder conversationText = new StringBuilder();
        int effective = 0;
        for (Message m : chat.getMessages()) {
            String role = m.getRole();
            String content = m.getContent();
            if (StringUtils.isBlank(content)) continue;

            if (Message.ROLE_USER.equals(role)) {
                conversationText.append("用户: ").append(content).append(NN);
                effective++;
            } else if (Message.ROLE_AI.equals(role)) {
                conversationText.append("AI: ").append(content).append(NN);
                effective++;
            }
        }
        if (effective == 0) return null;

        // 只保留最近内容，避免超出模型上下文
        if (conversationText.length() > SUMMARY_MAX_CHARS) {
            conversationText.delete(0, conversationText.length() - SUMMARY_MAX_CHARS);
        }

        String prompt = "你是对话压缩助手，请将以下对话历史压缩为两段摘要，保留关键信息和上下文要点。"
                + "<short> 标签内输出简短概要（100 字以内），<long> 标签内输出详细摘要（500 字以内），"
                + "除这两个标签外不要输出任何内容";
        ChatCompletionCreateParams.Builder builder = Config.createBuilder(prompt, chat.getAgent())
                .addUserMessage(conversationText.toString());

        ChatCompletion resp = ChatExecutor.createChat(builder.build(), null, chat.getAgent());
        if (resp.choices() == null || resp.choices().isEmpty()) return null;

        String content = resp.choices().get(0).message().content().orElse(null);
        String shortText = StringUtils.trimToNull(StringUtils.substringBetween(content, "<short>", "</short>"));
        String longText = StringUtils.trimToNull(StringUtils.substringBetween(content, "<long>", "</long>"));
        if (shortText == null || longText == null) {
            log.warn("Chat summary output missing tags : {}", chatid);
            return null;
        }

        JSONObject summary = new JSONObject(true);
        summary.put("short", shortText);
        summary.put("long", longText);
        return summary;
    }
}
