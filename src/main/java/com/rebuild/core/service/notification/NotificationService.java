/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.notification;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.InternalPersistService;
import com.rebuild.core.support.integration.SMSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 消息通知服务
 *
 * @author devezhao
 * @since 10/17/2018
 */
@Slf4j
@Service
public class NotificationService extends InternalPersistService {

    protected NotificationService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.Notification;
    }

    @Override
    public Record create(Record record) {
        record.setBoolean("unread", true);
        record = super.create(record);
        cleanCache(record.getPrimary());
        return record;
    }

    @Override
    public Record update(Record record) {
        cleanCache(record.getPrimary());
        return super.update(record);
    }

    @Override
    public int delete(ID recordId) {
        cleanCache(recordId);
        return super.delete(recordId);
    }

    // 清理缓存
    private void cleanCache(ID messageId) {
        Object[] m = Application.getQueryFactory().uniqueNoFilter(messageId, "toUser");
        if (m != null) {
            final String ckey = "UnreadNotification-" + m[0];
            Application.getCommonsCache().evict(ckey);
        }
    }

    // --

    /**
     * 发送消息
     *
     * @param message
     */
    public void send(Message message) {
        send(message, false, false);
    }

    /**
     * 发送消息
     *
     * @param message
     * @param distSms 同时发短信
     * @param distEmail 同时发邮件
     */
    public void send(Message message, boolean distSms, boolean distEmail) {
        Record record = EntityHelper.forNew(EntityHelper.Notification, message.getFromUser());
        record.setID("fromUser", message.getFromUser());
        record.setID("toUser", message.getToUser());
        record.setString("message", message.getMessage());
        if (message.getType() > 0) {
            record.setInt("type", message.getType());
        }
        if (message.getRelatedRecord() != null) {
            record.setID("relatedRecord", message.getRelatedRecord());
        }

        record = this.create(record);

        // 异步分发消息
        final ID messageId = record.getPrimary();
        ThreadPool.exec(() -> {
            String[] distNames = Application.getContext().getBeanNamesForType(MessageDistributor.class);
            for (String name : distNames) {
                MessageDistributor md = (MessageDistributor) Application.getContext().getBean(name);
                if (!md.isEnable()) {
                    String distType = md.getClass().getSimpleName();
                    if ("SmsDistributor".equals(distType)) {
                        if (!SMSender.availableSMS()) continue;
                        if (!distSms) continue;
                    } else if ("EmailDistributor".equals(distType)) {
                        if (!SMSender.availableMail()) continue;
                        if (!distEmail) continue;
                    } else {
                        continue;
                    }
                }

                try {
                    boolean sent = md.send(message, messageId);
                    log.info("Distribute message ({}) with {} : {}", sent ? "success" : "fails", md.getClass().getSimpleName(), message);
                } catch (Exception ex) {
                    log.error("Distribute message error : {}", message, ex);
                }
            }
        });
    }

    /**
     * 获取未读消息数
     *
     * @param user
     * @return
     */
    public int getUnreadCount(ID user) {
        final String ckey = "UnreadNotification-" + user;
        Object cval = Application.getCommonsCache().getx(ckey);
        if (cval != null) {
            return (Integer) cval;
        }

        Object[] unread = Application.createQueryNoFilter(
                "select count(messageId) from Notification where toUser = ? and unread = 'T'")
                .setParameter(1, user)
                .unique();
        int count = unread == null ? 0 : ObjectUtils.toInt(unread[0]);
        Application.getCommonsCache().putx(ckey, count);
        return count;
    }

    /**
     * 设为已读
     *
     * @param messageId
     */
    public void makeRead(ID messageId) {
        Record record = EntityHelper.forUpdate(messageId, UserContextHolder.getUser());
        record.setBoolean("unread", false);
        this.update(record);
    }
}
