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
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.general.ObservableService;
import com.rebuild.core.service.notification.Message;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
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
        int d = super.delete(recordId);

        this.awareMentionDelete(recordId, false);
        return d;
    }

    /**
     * 内容中涉及的用户要加入 FeedsMention
     *
     * @param record
     * @param isNew
     * @see #awareMentionCreate(Record)
     */
    protected void awareMention(Record record, boolean isNew) {
        String content = record.getString("content");
        if (content == null || record.getID("feedsId") == null) return;

        // 已存在的
        Set<ID> existsAtUsers = isNew ? Collections.emptySet() : this.awareMentionDelete(record.getPrimary(), true);

        Set<ID> atUsers = this.awareMentionCreate(record);
        if (atUsers.isEmpty()) return;

        // 发送通知
        final String msgContent = Language.L("@%s 在动态中提到了你", record.getEditor()) + " \n> " + content;
        ID related = record.getPrimary();
        if (related.getEntityCode() == EntityHelper.FeedsComment) {
            related = record.getID("feedsId");
        }

        if (atUsers.contains(UserService.ALLUSERS)
                && !existsAtUsers.contains(UserService.ALLUSERS)) {
            atUsers.clear();
            for (User u : Application.getUserStore().getAllUsers()) {
                if (u.isActive()) atUsers.add(u.getId());
            }
        }

        for (ID to : atUsers) {
            if (existsAtUsers.contains(to)) continue;
            Application.getNotifications().send(
                    MessageBuilder.createMessage(to, msgContent, Message.TYPE_FEEDS, related));
        }
    }

    /**
     * @param record
     * @return
     */
    protected Set<ID> awareMentionCreate(Record record) {
        final Record mention = EntityHelper.forNew(EntityHelper.FeedsMention, UserService.SYSTEM_USER);
        mention.setID("feedsId", record.getID("feedsId"));
        // can be null
        if (record.getEntity().containsField("commentId")) {
            mention.setID("commentId", record.getID("commentId"));
        }

        String fakeContent = record.getString("content");

        String atAllKey = "@" + Language.L("全部用户");
        if (fakeContent.contains(atAllKey)
                && Application.getPrivilegesManager().allow(UserContextHolder.getUser(), ZeroEntry.AllowAtAllUsers)) {
            fakeContent = fakeContent.replace(atAllKey, "@" + UserService.ALLUSERS);
        }

        Set<ID> atUsers = new HashSet<>();
        Matcher atMatcher = MessageBuilder.AT_PATTERN.matcher(fakeContent);

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

        return atUsers;
    }

    // 删除动态提及
    private Set<ID> awareMentionDelete(ID feedsOrComment, boolean needReturns) {
        Entity entity = MetadataHelper.getEntity(EntityHelper.FeedsMention);
        String whichField = feedsOrComment.getEntityCode() == EntityHelper.FeedsComment ? "commentId" : "feedsId";

        Set<ID> existsAtUsers = null;
        if (needReturns) {
            String sql = String.format("select user from %s where %s = '%s'",
                    entity.getName(), entity.getField(whichField).getName(), feedsOrComment);
            Object[][] array = Application.createQueryNoFilter(sql).array();

            existsAtUsers = new HashSet<>();
            for (Object[] o : array) {
                existsAtUsers.add((ID) o[0]);
            }
        }

        String dql = String.format("delete from `%s` where `%s` = '%s'",
                entity.getPhysicalName(), entity.getField(whichField).getPhysicalName(), feedsOrComment);
        Application.getSqlExecutor().execute(dql);

        return existsAtUsers;
    }

    /**
     * 将 @FULL_NAME 转成 @ID
     *
     * @param record
     * @return
     */
    private Record converContent(Record record) {
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
