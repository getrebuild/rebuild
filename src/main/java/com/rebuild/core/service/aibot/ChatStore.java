/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.query.QueryHelper;

/**
 * @author Zixin
 * @since 2025/4/18
 */
public class ChatStore {

    public static final ChatStore instance = new ChatStore();

    private ChatStore() {}

    /**
     * @param completions
     * @return
     */
    public MessageCompletions store(MessageCompletions completions) {
        return store(completions, null);
    }

    /**
     *
     * @param completions
     * @param user
     * @return
     */
    public MessageCompletions store(MessageCompletions completions, ID user) {
        final boolean isNew = completions.getChatid() == null;
        if (user == null) user = UserContextHolder.getUser();

        Record chat;
        if (isNew) {
            chat = EntityHelper.forNew(EntityHelper.AibotChat, user);
            String s = completions.getSubject();
            if (s == null) s = "会话:" + System.currentTimeMillis();
            chat.setString("subject", s);
        } else {
            chat = EntityHelper.forUpdate(completions.getChatid(), user);
        }

        JSONArray contents = new JSONArray();
        completions.getMessages().forEach(m -> contents.add(m.toClientJSON()));
        chat.setString("contents", contents.toJSONString());
        Application.getCommonsService().createOrUpdate(chat);

        if (isNew) {
            completions.setChatid(chat.getPrimary());
            completions.setSubject(chat.getString("subject"));
        }

        // 缓存
        Application.getCommonsCache().putx("chat-" + chat.getPrimary(), completions);
        return completions;
    }

    /**
     * @param chatid
     * @return
     */
    public MessageCompletions get(ID chatid) {
        final String key = "chat-" + chatid;
        MessageCompletions c = (MessageCompletions) Application.getCommonsCache().getx(key);
        if (c != null) return c;

        c = new MessageCompletions(null);
        Object o = QueryHelper.queryFieldValue(chatid, "contents");
        if (o != null) {
            JSONArray contents = JSON.parseArray((String) o);
            for (Object item : contents) {
                c.setRawMessage((JSONObject) item);
            }
        }
        return c;
    }

    /**
     * @param chatid
     */
    public void delete(ID chatid) {
        Application.getCommonsService().delete(chatid);
        Application.getCommonsCache().evict("chat-" + chatid);
    }
}
