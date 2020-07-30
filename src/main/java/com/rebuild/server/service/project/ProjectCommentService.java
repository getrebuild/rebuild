/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.project;

import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.business.feeds.FeedsHelper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.notification.Message;
import com.rebuild.server.service.notification.MessageBuilder;
import org.apache.commons.lang.StringUtils;

/**
 * @author devezhao
 * @since 2020/7/27
 */
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
        final ID user = Application.getCurrentUser();
        checkMember(user, record.getID("taskId"));

        record = super.create(record);

        chechAtUserAndNotification(record, record.getString("content"));
        awareAttachment(OperatingContext.create(
                Application.getCurrentUser(), BizzPermission.CREATE, null, record));
        return record;
    }

    @Override
    public Record update(Record record) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(ID commentId) {
        final ID user = Application.getCurrentUser();
        final Record beforeRecord = getBeforeRecord(commentId);

        ID createdBy = beforeRecord.getID(EntityHelper.CreatedBy);
        if (!(UserHelper.isAdmin(user) || user.equals(createdBy))) {
            throw new PrivilegesException("不能删除他人评论");
        }

        int d = super.delete(commentId);

        awareAttachment(OperatingContext.create(
                Application.getCurrentUser(), BizzPermission.DELETE, beforeRecord, null));
        return d;
    }

    /**
     * 检查指定内容中是否 AT 了其他用户，如果有就发通知
     *
     * @param record
     * @param content
     * @return
     */
    private int chechAtUserAndNotification(Record record, String content) {
        if (StringUtils.isBlank(content)) return 0;

        final String msg = "@" + record.getEditor() + " 在任务中提到了你 \n> " + content;

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
