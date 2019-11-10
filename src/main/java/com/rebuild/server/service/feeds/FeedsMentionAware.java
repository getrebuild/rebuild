/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.service.feeds;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.business.feeds.FeedsHelper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.BaseService;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.notification.Message;
import com.rebuild.server.service.notification.MessageBuilder;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * @author ZHAO
 * @since 2019/11/5
 */
public abstract class FeedsMentionAware extends BaseService {

    protected FeedsMentionAware(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public Record create(Record record) {
        record = super.create(converContent(record));
        aware(record, true);
        return record;
    }

    @Override
    public Record update(Record record) {
        record = super.update(converContent((record)));
        aware(record, false);
        return record;
    }

    @Override
    public int delete(ID recordId) {
        int del = super.delete(recordId);
        aware(recordId);
        return del;
    }

    /**
     * 内容中涉及的用户要加入 FeedsMention
     *
     * @param record
     */
    protected void aware(Record record, boolean isNew) {
        String content = record.getString("content");
        if (content == null || record.getID("feedsId") == null) {
            return;
        }

        if (!isNew) {
            this.aware(record.getPrimary());
        }

        final Record mention = EntityHelper.forNew(EntityHelper.FeedsMention, UserService.SYSTEM_USER);
        mention.setID("feedsId", record.getID("feedsId"));
        // Can be null
        if (record.getEntity().containsField("commentId")) {
            mention.setID("commentId", record.getID("commentId"));
        }

        Set<ID> atUsers = new HashSet<>();
        Matcher atMatcher = MessageBuilder.AT_PATTERN.matcher(content);
        while (atMatcher.find()) {
            String at = atMatcher.group().substring(1);
            ID atUser = ID.valueOf(at);
            if (atUser.getEntityCode() != EntityHelper.User) continue;

            Record clone = mention.clone();
            clone.setID("user", atUser);
            super.create(clone);
            atUsers.add(atUser);
        }
        if (atUsers.isEmpty()) return;

        // 发送通知
        String messageContent = String.format("@%s 在%s中提到了你",
                record.getEditor(),
                record.getEntity().getEntityCode() == EntityHelper.Feeds ? "动态" : "评论");
        messageContent += "\n> " + content;
        ID releated = record.getPrimary();
        if (releated.getEntityCode() == EntityHelper.FeedsComment) {
            releated = record.getID("feedsId");
        }

        for (ID to : atUsers) {
            Message message = new Message(
                    null, to, messageContent, releated, Message.TYPE_FEEDS);
            Application.getNotifications().send(message);
        }
    }

    /**
     * 内容中涉及的用户要移除 FeedsMention
     *
     * @param deleted
     */
    protected void aware(ID deleted) {
        Entity entity = MetadataHelper.getEntity(EntityHelper.FeedsMention);
        String whichField = deleted.getEntityCode() == EntityHelper.FeedsComment ? "commentId" : "feedsId";

        String dql = String.format("delete from `%s` where `%s` = '%s'",
                entity.getPhysicalName(), entity.getField(whichField).getPhysicalName(), deleted);
        Application.getSQLExecutor().execute(dql);
    }

    /**
     * 将 @FULL_NAME 转成 @ID
     *
     * @param record
     * @return
     */
    protected Record converContent(Record record) {
        String content = record.getString("content");
        if (StringUtils.isBlank(content)) return record;

        Map<String, ID> map = FeedsHelper.findMentionsMap(content);
        for (Map.Entry<String, ID> e : map.entrySet()) {
            content = content.replace("@" + e.getKey(), "@" + e.getValue());
        }
        record.setString("content", content);
        return record;
    }
}
