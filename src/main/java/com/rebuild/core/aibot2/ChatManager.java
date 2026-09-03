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
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.rebuild.core.metadata.EntityHelper.AibotChat;

/**
 * @author Zixin
 * @since 2025/11/1
 */
@Slf4j
public abstract class ChatManager {

    /**
     * @param user
     * @return
     */
    public static ID initChat(ID user, String subject) {
        Record chat = EntityHelper.forNew(AibotChat, user);

        if (StringUtils.isBlank(subject)) subject = "新会话";
        else subject = CommonsUtils.maxstr(subject, 40);
        chat.setString("subject", subject);

        chat = Application.getCommonsService().createOrUpdate(chat);
        return chat.getPrimary();
    }

    /**
     * @param chatid
     * @return
     */
    public static Chat getChat(ID chatid) {
        String ckey = "chat2-" + chatid;
        Serializable chat = Application.getCommonsCache().getx(ckey);
        if (chat == null) {
            chat = new Chat(chatid);
            Application.getCommonsCache().putx(ckey, chat);
        }
        return (Chat) chat;
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

        long tokenUsage = chat.getTokenUsage();
        r.setLong("token", tokenUsage > 0 ? tokenUsage : contents2s.length());
        Application.getCommonsService().createOrUpdate(r);
    }

    /**
     * @param chatid
     */
    public static void deleteChat(ID chatid) {
        String ckey = "chat2-" + chatid;
        Application.getCommonsCache().evict(ckey);
        Application.getCommonsService().delete(chatid);
    }

    /**
     * 直接提问/回答
     *
     * @param userContent
     * @return
     */
    public static String ask(String userContent) {
        return ask(userContent, null, null, null);
    }

    /**
     * 直接提问/回答（支持提示词、图片视觉识别）
     * 内部调用，落库归属 AI 助手
     *
     * @param userContent
     * @param prompt
     * @param files
     * @param source
     * @return
     */
    public static String ask(String userContent, String prompt, List<File> files, String source) {
        String subject = "ASK:" + (StringUtils.isBlank(source) ? "N" : source) + ":" + userContent;
        ID chatid = initChat(UserService.AIBOT_USER, subject);
        Chat chat = new Chat(chatid, null, prompt);

        String result;
        try {
            if (CollectionUtils.isEmpty(files)) {
                result = chat.ask(userContent);
            } else {
                // 通过 AI 视觉能力识别图片内容并返回文本描述（支持多张图片）
                List<ChatCompletionContentPart> parts = new ArrayList<>();
                parts.add(ChatCompletionContentPart.ofText(
                        ChatCompletionContentPartText.builder().text(userContent).build()));

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

                result = chat.ask(userContent, parts);
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
        return result;
    }

    /**
     * 以指定用户身份提问/回答（会话归属该用户，工具按其权限执行）
     *
     * @param userContent
     * @param source
     * @param user
     * @return
     */
    public static String askAsUser(String userContent, String prompt, List<File> files, String source, ID user) {
        Assert.notNull(user, "[user] cannot be null");
        ID keepCurrentUser = UserContextHolder.setUser(user);
        try {
            return ask(userContent, prompt, files, source);
        } finally {
            UserContextHolder.clearUser(keepCurrentUser);
        }
    }
}
