/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.RegexUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
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
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RbvFunction;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.general.ContentWithFieldVars;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.utils.MarkdownUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FIXME 异步发送可能失败，日志表未记录
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@Slf4j
public class SendNotification extends TriggerAction {

    private static final int MTYPE_NOTIFICATION = 1;    // 通知
    private static final int MTYPE_MAIL = 2;            // 邮件
    private static final int MTYPE_SMS = 3;             // 短信
    private static final int MTYPE_WXWORK = 4;          // 企微群
    private static final int MTYPE_DINGTALK = 5;        // 钉钉群
    private static final int UTYPE_USER = 1;            // 内部用户
    private static final int UTYPE_ACCOUNT = 2;         // 外部人员
    private static final int UTYPE_ACCOUNT20 = 20;      // 外部人员-输入
    private static final int UTYPE_WXWORK = 4;          // 企微群
    private static final int UTYPE_DINGTALK = 5;        // 钉钉群
    private static final int UTYPE_FEISHU = 6;          // 飞书群

    public SendNotification(ActionContext context) {
        super(context);
    }

    @Override
    public ActionType getType() {
        return ActionType.SENDNOTIFICATION;
    }

    @Override
    public Object execute(OperatingContext operatingContext) {
        final JSONObject content = (JSONObject) actionContext.getActionContent();
        final int msgType = content.getIntValue("type");
        final int userType = content.getIntValue("userType");

        if (msgType == MTYPE_MAIL && !SMSender.availableMail()) {
            return TriggerResult.wran("email-service unavailable");
        } else if (msgType == MTYPE_SMS && !SMSender.availableSMS()) {
            return TriggerResult.wran("sms-service unavailable");
        } else if (msgType == MTYPE_WXWORK && RebuildConfiguration.get(ConfigurationItem.WxworkCorpid) == null) {
            return TriggerResult.wran("wxwork-service unavailable");
        } else if (msgType == MTYPE_DINGTALK && RebuildConfiguration.get(ConfigurationItem.DingtalkRobotCode) == null) {
            return TriggerResult.wran("dingtalk-service unavailable");
        } else if (msgType == MTYPE_NOTIFICATION) {
            // default
        }

        Set<Object> s;
        if (userType == UTYPE_ACCOUNT || userType == UTYPE_ACCOUNT20) {
            s = sendToAccounts(operatingContext, userType);
        } else if (userType == UTYPE_WXWORK) {
            s = sendToWxwork(operatingContext);
        } else if (userType == UTYPE_DINGTALK) {
            s = sendToDingtalk(operatingContext);
        } else if (userType == UTYPE_FEISHU) {
            s = sendToFeishu(operatingContext);
        } else {  // UTYPE_USER
            s = sendToUsers(operatingContext);
        }

        if (s == null || s.isEmpty()) return TriggerResult.wran("No users");
        log.info("Sent notification to : {} with {}", s, actionContext.getConfigId());

        return TriggerResult.success(StringUtils.join(s, ","));
    }

    private Set<Object> sendToUsers(OperatingContext operatingContext) {
        final JSONObject content = (JSONObject) actionContext.getActionContent();
        final int msgType = content.getIntValue("type");

        Set<ID> toUsers = UserHelper.parseUsers(
                content.getJSONArray("sendTo"), actionContext.getSourceRecord(), Boolean.TRUE);
        if (toUsers.isEmpty()) return null;

        String[] message = formatMessageContent(actionContext, operatingContext);
        Set<Object> send = new HashSet<>();

        String emailContent = null;
        File[] emailAttach = null;
        if (msgType == MTYPE_MAIL) {
            emailContent = MarkdownUtils.render(message[0], false, true);
            emailAttach = getMailAttach(content);
        }

        // v4.1 合并发送。需要邮件服务器支持，否则还是会单个发送
        if (msgType == MTYPE_MAIL && content.getBooleanValue("mergeSend")) {
            for (ID user : toUsers) {
                String emailAddr = Application.getUserStore().getUser(user).getEmail();
                if (RegexUtils.isEMail(emailAddr)) send.add(emailAddr);
            }

            if (!send.isEmpty()) {
                SMSender.sendMailAsync(StringUtils.join(send, ","), message[1], emailContent, emailAttach);
            }
            return send;
        }

        for (ID user : toUsers) {
            if (send.contains(user)) continue;

            if (msgType == MTYPE_MAIL) {
                String emailAddr = Application.getUserStore().getUser(user).getEmail();
                if (RegexUtils.isEMail(emailAddr)) {
                    SMSender.sendMailAsync(emailAddr, message[1], emailContent, emailAttach);
                    send.add(emailAddr);
                }
            }

            if (msgType == MTYPE_SMS) {
                String mobileAddr = Application.getUserStore().getUser(user).getWorkphone();
                if (RegexUtils.isCNMobile(mobileAddr)) {
                    SMSender.sendSMSAsync(mobileAddr, message[0]);
                    send.add(mobileAddr);
                }
            }

            if (msgType == MTYPE_NOTIFICATION) {
                Message m = MessageBuilder.createMessage(user, message[0], Message.TYPE_DEFAULT, actionContext.getSourceRecord());
                Application.getNotifications().send(m);
                send.add(user);
            }
        }
        return send;
    }

    private Set<Object> sendToAccounts(OperatingContext operatingContext, int userType) {
        final JSONObject content = (JSONObject) actionContext.getActionContent();
        final int msgType = content.getIntValue("type");

        Object[] to = null;
        if (userType == UTYPE_ACCOUNT20) {
            to = content.getString("sendTo").split("[，,;；]");
        } else {
            String[] validFields = getValidDefsFields(content.getJSONArray("sendTo"));
            if (validFields == null) return null;

            // v3.4 删除就尝试从快照中取
            if (operatingContext.getAction() == BizzPermission.DELETE) {
                Record beforeRecord = operatingContext.getBeforeRecord();
                if (beforeRecord != null) {
                    List<String> toList = new ArrayList<>();
                    for (String s : validFields) {
                        Object v;
                        if ((v = beforeRecord.getObjectValue(s)) != null) toList.add(v.toString());
                    }
                    to = toList.toArray(new String[0]);
                }
            } else {
                to = Application.getQueryFactory().uniqueNoFilter(actionContext.getSourceRecord(), validFields);
            }
        }
        if (to == null) return null;

        String[] message = formatMessageContent(actionContext, operatingContext);
        Set<Object> send = new HashSet<>();

        String emailContent = null;
        File[] emailAttach = null;
        if (msgType == MTYPE_MAIL) {
            emailContent = MarkdownUtils.render(message[0], false, true);
            emailAttach = getMailAttach(content);
        }

        // v4.1 合并发送
        if (msgType == MTYPE_MAIL && content.getBooleanValue("mergeSend")) {
            for (Object me : to) {
                String emailAddr = me == null ? null : me.toString().trim();
                if (RegexUtils.isEMail(emailAddr)) send.add(emailAddr);
            }

            if (!send.isEmpty()) {
                SMSender.sendMailAsync(StringUtils.join(send, ","), message[1], emailContent, emailAttach);
            }
            return send;
        }

        for (Object me : to) {
            if (me == null) continue;

            String mobileOrEmail = me.toString().trim();
            if (send.contains(mobileOrEmail)) continue;

            if (msgType == MTYPE_SMS && RegexUtils.isCNMobile(mobileOrEmail)) {
                SMSender.sendSMSAsync(mobileOrEmail, message[0]);
                send.add(mobileOrEmail);
            }

            if (msgType == MTYPE_MAIL && RegexUtils.isEMail(mobileOrEmail)) {
                SMSender.sendMailAsync(mobileOrEmail, message[1], emailContent, emailAttach);
                send.add(mobileOrEmail);
            }
        }
        return send;
    }

    private Set<Object> sendToWxwork(OperatingContext operatingContext) {
        return RbvFunction.call().sendToWxwork(actionContext, operatingContext);
    }

    private Set<Object> sendToDingtalk(OperatingContext operatingContext) {
        return RbvFunction.call().sendToDingtalk(actionContext, operatingContext);
    }

    private Set<Object> sendToFeishu(OperatingContext operatingContext) {
        return RbvFunction.call().sendToFeishu(actionContext, operatingContext);
    }

    private File[] getMailAttach(final JSONObject content) {
        String[] attachFields = getValidDefsFields(content.getJSONArray("attach"));
        if (attachFields == null) return null;

        Object[] o = Application.getQueryFactory().unique(actionContext.getSourceRecord(), attachFields);
        if (o == null || o.length == 0) return null;

        List<File> files = new ArrayList<>();
        for (Object item : o) {
            if (item == null || item instanceof ID) continue;

            JSONArray paths = JSON.parseArray((String) item);
            for (Object path : paths) {
                try {
                    files.add(QiniuCloud.getStorageFile((String) path));
                } catch (IOException ex) {
                    log.warn("Cannot get storage file : {}", path, ex);
                }
            }
        }
        return files.toArray(new File[0]);
    }

    private String[] getValidDefsFields(JSONArray defs) {
        if (defs == null || defs.isEmpty()) return null;

        List<String> validFields = new ArrayList<>();
        for (Object field : defs) {
            if (MetadataHelper.getLastJoinField(actionContext.getSourceEntity(), field.toString()) != null) {
                validFields.add(field.toString());
            }
        }
        return validFields.isEmpty() ? null : validFields.toArray(new String[0]);
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
