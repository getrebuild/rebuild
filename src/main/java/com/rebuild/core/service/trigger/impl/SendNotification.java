/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.ThreadPool;
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
import com.rebuild.core.service.trigger.TriggerResult;
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
    public Object execute(OperatingContext operatingContext) {
        if (operatingContext.getAction() == BizzPermission.UPDATE && !hasUpdateFields(operatingContext)) {
            return TriggerResult.wran("No update fields");
        }

        final JSONObject content = (JSONObject) actionContext.getActionContent();
        final int msgType = content.getIntValue("type");
        final int userType = content.getIntValue("userType");

        if (msgType == MTYPE_MAIL && !SMSender.availableMail()) {
            return TriggerResult.wran("email-service unavailable");
        } else if (msgType == MTYPE_SMS && !SMSender.availableSMS()) {
            return TriggerResult.wran("sms-service unavailable");
        } else if (msgType == MTYPE_NOTIFICATION) {
            // default
        }

        int s;
        if (userType == UTYPE_ACCOUNT) {
            s = sendToAccounts(operatingContext);
        } else {  // UTYPE_USER
            s = sendToUsers(operatingContext);
        }
        log.info("Sent notification : {} with {}", s, actionContext.getConfigId());

        return TriggerResult.success(String.valueOf(s));
    }

    private int sendToUsers(OperatingContext operatingContext) {
        final JSONObject content = (JSONObject) actionContext.getActionContent();
        final int msgType = content.getIntValue("type");

        Set<ID> toUsers = UserHelper.parseUsers(
                content.getJSONArray("sendTo"), actionContext.getSourceRecord(), Boolean.TRUE);
        if (toUsers.isEmpty()) return -1;

        String[] message = formatMessageContent(actionContext, operatingContext);
        int send = 0;

        for (ID user : toUsers) {
            if (msgType == MTYPE_MAIL) {
                String emailAddr = Application.getUserStore().getUser(user).getEmail();
                if (RegexUtils.isEMail(emailAddr)) {
                    SMSender.sendMailAsync(emailAddr, message[1], message[0]);
                    send++;
                }

            } else if (msgType == MTYPE_SMS) {
                String mobileAddr = Application.getUserStore().getUser(user).getWorkphone();
                if (RegexUtils.isCNMobile(mobileAddr)) {
                    SMSender.sendSMSAsync(mobileAddr, message[0]);
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
        final int msgType = content.getIntValue("type");

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

        String[] message = formatMessageContent(actionContext, operatingContext);
        int send = 0;

        for (Object item : o) {
            if (item == null) continue;

            String mobileOrEmail = item.toString();
            if (msgType == MTYPE_SMS && RegexUtils.isCNMobile(mobileOrEmail)) {
                SMSender.sendSMSAsync(mobileOrEmail, message[0]);
                send++;
            }
            if (msgType == MTYPE_MAIL && RegexUtils.isEMail(mobileOrEmail)) {
                SMSender.sendMailAsync(mobileOrEmail, message[1], message[0]);
                send++;
            }
        }
        return send;
    }

    // --

    /**
     * 处理消息内容
     *
     * @param actionContext
     * @param operatingContext
     * @return
     */
    public static String[] formatMessageContent(ActionContext actionContext, OperatingContext operatingContext) {
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
