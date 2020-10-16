/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.feeds;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.ObservableService;
import com.rebuild.core.service.notification.Message;
import com.rebuild.core.service.notification.MessageBuilder;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * 从动态/评论中提取 at 用户，以及将文件放置在福建表
 *
 * @author ZHAO
 * @since 2019/11/5
 */
public abstract class BaseFeedsService extends ObservableService {

    protected BaseFeedsService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public Record create(Record record) {
        record = super.create(converContent(record));

        awareMention(record, true);
        return record;
    }

    @Override
    public Record update(Record record) {
        record = super.update(converContent((record)));

        awareMention(record, false);
        return record;
    }

    @Override
    public int delete(ID recordId) {
        int del = super.delete(recordId);

        awareMention(recordId);
        return del;
    }

    /**
     * 内容中涉及的用户要加入 FeedsMention
     *
     * @param record
     * @param isNew
     */
    protected void awareMention(Record record, boolean isNew) {
        String content = record.getString("content");
        if (content == null || record.getID("feedsId") == null) {
            return;
        }

        if (!isNew) {
            this.awareMention(record.getPrimary());
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
            if (atUser.getEntityCode() != EntityHelper.User || atUsers.contains(atUser)) {
                continue;
            }

            Record clone = mention.clone();
            clone.setID("user", atUser);
            Application.getCommonsService().create(clone);
            atUsers.add(atUser);
        }

        if (atUsers.isEmpty()) return;

        // 发送通知
        final String msgContent = "@" + record.getEditor() + " 在动态中提到了你 \n> " + content;
        ID related = record.getPrimary();
        if (related.getEntityCode() == EntityHelper.FeedsComment) {
            related = record.getID("feedsId");
        }

        for (ID to : atUsers) {
            Application.getNotifications().send(
                    MessageBuilder.createMessage(to, msgContent, Message.TYPE_FEEDS, related));
        }
    }

    /**
     * 内容中涉及的用户要移除 FeedsMention
     *
     * @param deleted
     */
    protected void awareMention(ID deleted) {
        Entity entity = MetadataHelper.getEntity(EntityHelper.FeedsMention);
        String whichField = deleted.getEntityCode() == EntityHelper.FeedsComment ? "commentId" : "feedsId";

        String dql = String.format("delete from `%s` where `%s` = '%s'",
                entity.getPhysicalName(), entity.getField(whichField).getPhysicalName(), deleted);
        Application.getSqlExecutor().execute(dql);
    }

    /**
     * 将 @FULL_NAME 转成 @ID
     *
     * @param record
     * @return
     */
    private Record converContent(Record record) {
        String content = record.getString("content");
        if (StringUtils.isBlank(content)) {
            return record;
        }

        Map<String, ID> map = FeedsHelper.findMentionsMap(content);
        for (Map.Entry<String, ID> e : map.entrySet()) {
            content = content.replace("@" + e.getKey(), "@" + e.getValue());
        }
        record.setString("content", content);
        return record;
    }
}
