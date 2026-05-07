/*!
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
import com.rebuild.core.service.TransactionManual;
import com.rebuild.core.service.aibot2.ChatManager;
import com.rebuild.core.service.general.ObservableService;
import com.rebuild.core.service.notification.Message;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.general.RecordBuilder;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public abstract class BaseFeedsService extends ObservableService {

    // 全部用户（注意这是一个虚拟用户 ID，并不真实存在）
    public static final ID USER_ALLS = ID.valueOf("001-9999999999999999");
    // v4.4 AI 助手用户（注意这是一个虚拟用户 ID，并不真实存在）
    public static final ID USER_AIBOT = ID.valueOf("001-9999999999999998");

    protected BaseFeedsService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public Record create(Record record) {
        record = super.create(converContent4Mentions(record));

        awareMention(record, true);
        return record;
    }

    @Override
    public Record update(Record record) {
        record = super.update(converContent4Mentions(record));

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

        // 发布人
        ID publishUser = record.getEditor();

        // 发送通知
        final String msgContent = Language.L("@%s 在动态中提到了你", publishUser) + " \n> " + content;
        ID related = record.getPrimary();
        if (related.getEntityCode() == EntityHelper.FeedsComment) {
            related = record.getID("feedsId");
        }

        if (atUsers.contains(USER_ALLS) && !existsAtUsers.contains(USER_ALLS)) {
            atUsers.clear();
            for (User u : Application.getUserStore().getAllUsers()) {
                if (u.isActive()) atUsers.add(u.getId());
            }
        }

        if (atUsers.contains(USER_AIBOT) && !existsAtUsers.contains(USER_AIBOT)) {
            TransactionManual.registerAfterCommit(() -> {
                String aiReply;
                try {
                    aiReply = ChatManager.ask("请尽量简短回答以下问题：\n" + content);
                } catch (Exception ex) {
                    log.error("AiBot error on ask", ex);
                    aiReply = "错误:" + CommonsUtils.getRootMessage(ex);
                }

                aiReply = StringUtils.trim(aiReply);
                aiReply += "\n\n@" + publishUser;

                Record r = RecordBuilder.builder(EntityHelper.FeedsComment)
                        .add("feedsId", record.getID("feedsId"))
                        .add("content", aiReply)
                        .build(UserService.SYSTEM_USER);

                UserContextHolder.setUser(UserService.SYSTEM_USER);
                try {
                    Application.getBean(FeedsCommentService.class).createOrUpdate(r);
                } finally {
                    UserContextHolder.clearUser();
                }
            });
        }

        if (CommandArgs.getBoolean(CommandArgs._DisNotificationFeeds)) return;
        for (ID to : atUsers) {
            if (existsAtUsers.contains(to)) continue;
            if (existsAtUsers.contains(USER_ALLS)) continue;
            if (existsAtUsers.contains(USER_AIBOT)) continue;

            Application.getNotifications().send(
                    MessageBuilder.createMessage(to, msgContent, Message.TYPE_FEEDS, related, record.getEditor()));
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

        Set<String> locales = Application.getLanguage().getAvailableLocales().keySet();
        if (Application.getPrivilegesManager().allow(getCurrentUser(), ZeroEntry.AllowAtAllUsers)) {
            for (String locale : locales) {
                String keyText = "@" + Application.getLanguage().getBundle(locale).L("所有人");
                if (fakeContent.contains(keyText)) {
                    fakeContent = fakeContent.replace(keyText, "@" + USER_ALLS);
                }
            }
        }
        if (Application.getPrivilegesManager().allow(getCurrentUser(), ZeroEntry.AllowUseAiBot)) {
            for (String locale : locales) {
                String keyText = "@" + Application.getLanguage().getBundle(locale).L("AI 助手");
                if (fakeContent.contains(keyText)) {
                    fakeContent = fakeContent.replace(keyText, "@" + USER_AIBOT);
                }
            }
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
    private Record converContent4Mentions(Record record) {
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
