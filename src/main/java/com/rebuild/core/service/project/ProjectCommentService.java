/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.project;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.OperationDeniedException;
import com.rebuild.core.service.feeds.FeedsHelper;
import com.rebuild.core.service.notification.Message;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

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
        final ID user = UserContextHolder.getUser();
        checkInMembers(user, record.getID("taskId"));

        record = super.create(record);

        checkAtUserAndNotification(record, record.getString("content"));
        return record;
    }

    @Override
    public Record update(Record record) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(ID commentId) {
        final ID user = UserContextHolder.getUser();
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
    private int checkAtUserAndNotification(Record record, String content) {
        if (StringUtils.isBlank(content)) return 0;

        final String msg = Language.L("@%s 在任务中提到了你", record.getEditor()) + " \n> " + content;

        ID[] atUsers = FeedsHelper.findMentions(content);
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
                    MessageBuilder.createMessage(to, msg, Message.TYPE_PROJECT, record.getPrimary()));
            send++;
        }
        return send;
    }
}
