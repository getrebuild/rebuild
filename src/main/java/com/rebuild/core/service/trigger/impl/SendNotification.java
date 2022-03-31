/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.notification.Message;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.support.general.ContentWithFieldVars;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.SMSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Set;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@Slf4j
public class SendNotification implements TriggerAction {

    // 通知
//    private static final int TYPE_NOTIFICATION = 1;
    // 邮件
    private static final int TYPE_MAIL = 2;
    // 短信
    private static final int TYPE_SMS = 3;

    final private ActionContext context;

    public SendNotification(ActionContext context) {
        this.context = context;
    }

    @Override
    public ActionType getType() {
        return ActionType.SENDNOTIFICATION;
    }

    @Override
    public void execute(OperatingContext operatingContext) {
        ThreadPool.exec(() -> {
            try {
                // FIXME 等待事物完成
                ThreadPool.waitFor(3000);

                executeAsync(operatingContext);
            } catch (Exception ex) {
                log.error(null, ex);
            }
        });
    }

    private void executeAsync(OperatingContext operatingContext) {
        final JSONObject content = (JSONObject) context.getActionContent();

        Set<ID> toUsers = UserHelper.parseUsers(content.getJSONArray("sendTo"), context.getSourceRecord());
        if (toUsers.isEmpty()) {
            return;
        }

        final int type = content.getIntValue("type");
        if (type == TYPE_MAIL && !SMSender.availableMail()) {
            log.warn("Could not send because email-service is unavailable");
            return;
        } else if (type == TYPE_SMS && !SMSender.availableSMS()) {
            log.warn("Could not send because sms-service is unavailable");
            return;
        }

        String message = content.getString("content");

        if (operatingContext.getAction() == BizzPermission.DELETE) {
            message = ContentWithFieldVars.replaceWithRecord(message, operatingContext.getBeforeRecord());
        } else {
            message = ContentWithFieldVars.replaceWithRecord(message, context.getSourceRecord());
        }

        String emailSubject = content.getString("title");
        if (StringUtils.isBlank(emailSubject)) emailSubject = Language.L("你有一条新通知");

        for (ID user : toUsers) {
            if (type == TYPE_MAIL) {
                String emailAddr = Application.getUserStore().getUser(user).getEmail();
                if (emailAddr != null) {
                    SMSender.sendMail(emailAddr, emailSubject, message);
                }

            } else if (type == TYPE_SMS) {
                String mobile = Application.getUserStore().getUser(user).getWorkphone();
                if (RegexUtils.isCNMobile(mobile)) {
                    SMSender.sendSMS(mobile, message);
                }

            } else {  // TYPE_NOTIFICATION
                Message m = MessageBuilder.createMessage(user, message, Message.TYPE_DEFAULT, context.getSourceRecord());
                Application.getNotifications().send(m);
            }
        }
    }
}
