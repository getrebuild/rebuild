/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartImage;
import com.openai.models.chat.completions.ChatCompletionContentPartText;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;

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

    private static final Tika TIKA = new Tika();

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
        r.setInt("token", contents2s.length());
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
        return ask(userContent, null, null);
    }

    /**
     * 直接提问/回答
     *
     * @param userContent
     * @param prompt
     * @return
     * @see Chat#ask(String)
     */
    public static String ask(String userContent, String prompt) {
        return ask(userContent, prompt, null);
    }

    /**
     * 直接提问/回答（支持图片视觉识别）
     *
     * @param userContent 用户内容
     * @param prompt      系统提示词（可为空）
     * @param imageFiles  图片文件列表（可为空，为空则按纯文本处理）
     * @return AI 回答内容
     */
    public static String ask(String userContent, String prompt, List<File> imageFiles) {
        if (imageFiles != null && !imageFiles.isEmpty()) {
            return askWithImage(userContent, prompt, imageFiles);
        }
        return new Chat(EntityHelper.newUnsavedId(AibotChat), prompt, null)
                .ask(userContent);
    }

    /**
     * 通过 AI 视觉能力识别图片内容并返回文本描述（支持多张图片）
     *
     * @param userContent 提示词
     * @param prompt     系统提示词
     * @param imageFiles 图片文件列表
     * @return AI 返回的文本描述
     */
    private static String askWithImage(String userContent, String prompt, List<File> imageFiles) {
        List<ChatCompletionContentPart> parts = new ArrayList<>();

        // 文本提示
        parts.add(ChatCompletionContentPart.ofText(
                ChatCompletionContentPartText.builder().text(userContent).build()));

        // 图片内容
        for (File imageFile : imageFiles) {
            String base64 = CommonsUtils.fileToBase64(imageFile);
            String mimeType;
            try {
                mimeType = TIKA.detect(imageFile);
            } catch (IOException e) {
                mimeType = "image/png";
                log.warn("Failed to detect image mime type, fallback to png : {}", imageFile.getName(), e);
            }
            String dataUrl = String.format("data:%s;base64,%s", mimeType, base64);

            parts.add(ChatCompletionContentPart.ofImageUrl(
                    ChatCompletionContentPartImage.builder()
                            .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
                                    .url(dataUrl).build())
                            .build()));
        }

        ChatCompletionCreateParams.Builder builder = Config.createBuilder(prompt, null)
                .addUserMessageOfArrayOfContentParts(parts);

        ChatCompletion resp = Config.getClient().chat().completions().create(builder.build());
        return resp.choices().get(0).message().content().orElse("");
    }
}
