/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@Slf4j
public class SendNotification extends TriggerAction {

    private static final int MTYPE_NOTIFICATION = 1;// 通知
    private static final int MTYPE_MAIL = 2;        // 邮件
    private static final int MTYPE_SMS = 3;         // 短信
    private static final int UTYPE_USER = 1;    // 内部用户
    private static final int UTYPE_ACCOUNT = 2; // 外部人员

    public SendNotification(ActionContext context) {
        super(context);
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
        final JSONObject content = (JSONObject) actionContext.getActionContent();

        // 指定字段
        JSONArray whenUpdateFields = content.getJSONArray("whenUpdateFields");
        if (operatingContext.getAction() == BizzPermission.UPDATE
                && whenUpdateFields != null && !whenUpdateFields.isEmpty()) {
            Record updatedRecord = operatingContext.getAfterRecord();
            boolean hasUpdated = false;
            for (String field : updatedRecord.getAvailableFields()) {
                if (whenUpdateFields.contains(field)) {
                    hasUpdated = true;
                    break;
                }
            }

            if (!hasUpdated) return;
        }

        final int type = content.getIntValue("type");
        final int userType = content.getIntValue("userType");

        if (type == MTYPE_MAIL && !SMSender.availableMail()) {
            log.warn("Could not send because email-service is unavailable");
            return;
        } else if (type == MTYPE_SMS && !SMSender.availableSMS()) {
            log.warn("Could not send because sms-service is unavailable");
            return;
        }

        int s;
        if (userType == UTYPE_ACCOUNT) {
            s = sendToAccounts(operatingContext);
        } else {  // UTYPE_USER
            s = sendToUsers(operatingContext);
        }
        log.info("Sent notification : {} with {}", s, actionContext.getConfigId());
    }

    private int sendToUsers(OperatingContext operatingContext) {
        final JSONObject content = (JSONObject) actionContext.getActionContent();
        final int type = content.getIntValue("type");

        Set<ID> toUsers = UserHelper.parseUsers(content.getJSONArray("sendTo"), actionContext.getSourceRecord());
        if (toUsers.isEmpty()) return -1;

        String[] message = getMessageContent(operatingContext);
        int send = 0;

        for (ID user : toUsers) {
            if (type == MTYPE_MAIL) {
                String emailAddr = Application.getUserStore().getUser(user).getEmail();
                if (emailAddr != null) {
                    SMSender.sendMail(emailAddr, message[1], message[0]);
                    send++;
                }

            } else if (type == MTYPE_SMS) {
                String mobile = Application.getUserStore().getUser(user).getWorkphone();
                if (RegexUtils.isCNMobile(mobile)) {
                    SMSender.sendSMS(mobile, message[0]);
                    send++;
                }

            } else {  // TYPE_NOTIFICATION
                Message m = MessageBuilder.createMessage(user, message[0], Message.TYPE_DEFAULT, actionContext.getSourceRecord());
                Application.getNotifications().send(m);
                send++;
            }
        }
        return send;
    }

    private int sendToAccounts(OperatingContext operatingContext) {
        final JSONObject content = (JSONObject) actionContext.getActionContent();
        final int type = content.getIntValue("type");

        JSONArray fieldsDef = content.getJSONArray("sendTo");
        if (fieldsDef == null || fieldsDef.isEmpty()) return -1;

        List<String> validFields = new ArrayList<>();
        for (Object field : fieldsDef) {
            if (MetadataHelper.getLastJoinField(actionContext.getSourceEntity(), field.toString()) != null) {
                validFields.add(field.toString());
            }
        }
        if (validFields.isEmpty()) return -1;

        Object[] o = Application.getQueryFactory().uniqueNoFilter(
                actionContext.getSourceRecord(), validFields.toArray(new String[0]));
        if (o == null) return -1;

        String[] message = getMessageContent(operatingContext);
        int send = 0;

        for (Object item : o) {
            if (item == null) continue;

            String mobileOrEmail = item.toString();
            if (type == MTYPE_SMS && RegexUtils.isCNMobile(mobileOrEmail)) {
                SMSender.sendSMS(mobileOrEmail, message[0]);
                send++;
            } else if (type == MTYPE_MAIL && RegexUtils.isEMail(mobileOrEmail)) {
                SMSender.sendMail(mobileOrEmail, message[1], message[0]);
                send++;
            }
        }
        return send;
    }

    private String[] getMessageContent(OperatingContext operatingContext) {
        final JSONObject content = (JSONObject) actionContext.getActionContent();

        String message = content.getString("content");
        String emailSubject = content.getString("title");
        if (StringUtils.isBlank(emailSubject)) emailSubject = Language.L("你有一条新通知");

        if (operatingContext.getAction() == BizzPermission.DELETE) {
            message = ContentWithFieldVars.replaceWithRecord(message, operatingContext.getBeforeRecord());
            emailSubject = ContentWithFieldVars.replaceWithRecord(emailSubject, operatingContext.getBeforeRecord());
        } else {
            message = ContentWithFieldVars.replaceWithRecord(message, actionContext.getSourceRecord());
            emailSubject = ContentWithFieldVars.replaceWithRecord(emailSubject, actionContext.getSourceRecord());
        }

        return new String[] { message, emailSubject };
    }
}
