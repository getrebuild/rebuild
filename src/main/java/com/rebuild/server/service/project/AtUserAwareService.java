/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.project;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.business.feeds.FeedsHelper;
import com.rebuild.server.service.BaseService;
import com.rebuild.server.service.notification.Message;
import com.rebuild.server.service.notification.MessageBuilder;
import org.apache.commons.lang.StringUtils;

/**
 * @author devezhao
 * @since 2020/7/27
 */
public abstract class AtUserAwareService extends BaseService {

    protected AtUserAwareService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    /**
     * 检查指定内容中是否 AT 了其他用户，如果有就发通知
     *
     * @param record
     * @param content
     * @return
     */
    protected int checkAtUserAndNotification(Record record, String content) {
        if (StringUtils.isBlank(content)) return 0;

        final String msg = "@" + record.getEditor() + " 在任务中提到了你 \n> " + content;

        ID[] atUsers = FeedsHelper.findMentions(content);
        int at = 0;
        for (ID to : atUsers) {
            Application.getNotifications().send(
                    MessageBuilder.createMessage(to, msg, Message.TYPE_PROJECT, record.getPrimary()));
            at++;
        }
        return at;
    }
}
