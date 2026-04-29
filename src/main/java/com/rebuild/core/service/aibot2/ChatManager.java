/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot2;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * @author Zixin
 * @since 2025/11/1
 */
public abstract class ChatManager {

    /**
     * @param user
     * @return
     */
    public static ID initChat(ID user, String subject) {
        Record chat = EntityHelper.forNew(EntityHelper.AibotChat, user);

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
        r.setString("contents", contents.toJSONString());
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
     * @param askContent
     * @return
     * @see Chat#ask(String)
     */
    public static String ask(String askContent) {
        return new Chat(EntityHelper.newUnsavedId(EntityHelper.AibotChat)).ask(askContent);
    }
}
