/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.project;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.OperationDeniedException;
import com.rebuild.core.service.feeds.FeedsHelper;
import com.rebuild.core.service.notification.Message;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author devezhao
 * @since 2020/7/27
 */
@Service
public class ProjectCommentService extends BaseTaskService {

    protected ProjectCommentService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.ProjectTaskComment;
    }

    @Override
    public Record create(Record record) {
        final ID user = getCurrentUser();
        checkModifications(user, record.getID("taskId"));

        String content = record.getString("content");
        // 将 @FULL_NAME 转成 @ID
        Map<String, ID> map = FeedsHelper.findMentionsMap(content);
        for (Map.Entry<String, ID> e : map.entrySet()) {
            content = content.replace("@" + e.getKey(), "@" + e.getValue());
        }
        record.setString("content", content);
        record = super.create(record);

        if (StringUtils.isNotBlank(content) && !map.isEmpty()) {
            checkAtUserAndNotification(record, content, map.values().toArray(new ID[0]));
        }
        return record;
    }

    @Override
    public Record update(Record record) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(ID commentId) {
        final ID user = getCurrentUser();
        if (!ProjectHelper.isManageable(commentId, user)) throw new OperationDeniedException();

        return super.delete(commentId);
    }

    /**
     * 检查指定内容中是否 AT 了其他用户，如果有就发通知
     *
     * @param record
     * @param content
     * @return
     */
    private int checkAtUserAndNotification(Record record, String content, ID[] atUsers) {
        if (CommandArgs.getBoolean(CommandArgs._DisNotificationTasks)) return 0;
        String msgContent = Language.L("@%s 在任务中提到了你", record.getEditor()) + " \n> " + content;
        ID related = record.getID("taskId");
        if (related == null) related = (ID) QueryHelper.queryFieldValue(record.getPrimary(), "taskId");

        int send = 0;
        for (ID to : atUsers) {
            // 是否已经发送过
            Object[] sent = Application.createQueryNoFilter(
                    "select messageId from Notification where toUser = ? and relatedRecord = ?")
                    .setParameter(1, to)
                    .setParameter(2, record.getPrimary())
                    .unique();
            if (sent != null) continue;

            Application.getNotifications().send(
                    MessageBuilder.createMessage(to, msgContent, Message.TYPE_PROJECT, related, record.getEditor()));
            send++;
        }
        return send;
    }
}
