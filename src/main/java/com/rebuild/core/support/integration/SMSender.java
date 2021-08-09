/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.integration;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SUBMAIL SMS/MAIL 发送
 *
 * @author devezhao
 * @since 01/03/2019
 */
@Slf4j
public class SMSender {

    private static final String STATUS_OK = "success";

    private static final int TYPE_SMS = 1;
    private static final int TYPE_EMAIL = 2;

    /**
     * @param to
     * @param subject
     * @param content
     */
    public static void sendMailAsync(String to, String subject, String content) {
        ThreadPool.exec(() -> {
            try {
                sendMail(to, subject, content);
            } catch (Exception ex) {
                log.error("Mail failed to send : " + to + " < " + subject, ex);
            }
        });
    }

    /**
     * @param to
     * @param subject
     * @param content
     * @return
     */
    public static String sendMail(String to, String subject, String content) {
        return sendMail(to, subject, content, true, RebuildConfiguration.getMailAccount());
    }

    /**
     * @param to
     * @param subject
     * @param content
     * @param useTemplate
     * @return <tt>null</tt> if failed or SENDID
     * @throws ConfigurationException If mail-account unset
     */
    public static String sendMail(String to, String subject, String content, boolean useTemplate, String[] specAccount) throws ConfigurationException {
        if (specAccount == null || specAccount.length < 4
                || StringUtils.isBlank(specAccount[0]) || StringUtils.isBlank(specAccount[1])
                || StringUtils.isBlank(specAccount[2]) || StringUtils.isBlank(specAccount[3])) {
            throw new ConfigurationException(Language.L("邮件账户未配置或配置错误"));
        }

        // 使用邮件模板
        if (useTemplate) {
            Element mailbody = getMailTemplate();

            mailbody.selectFirst(".rb-title").text(subject);
            mailbody.selectFirst(".rb-content").html(content);
            String htmlContent = mailbody.html();
            // 处理公共变量
            htmlContent = htmlContent.replace("%TO%", to);
            htmlContent = htmlContent.replace("%TIME%", CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now()));
            htmlContent = htmlContent.replace("%APPNAME%", RebuildConfiguration.get(ConfigurationItem.AppName));
            content = htmlContent;
        }

        final String logContent = "【" + subject + "】" + content;

        // Use SMTP
        if (specAccount.length >= 5 && StringUtils.isNotBlank(specAccount[4])) {
            String emailId;
            try {
                emailId = sendMailViaSmtp(to, subject, content, specAccount);
            } catch (EmailException ex) {
                log.error("Mail failed to send : " + to + " > " + subject, ex);
                return null;
            }

            createLog(to, logContent, TYPE_EMAIL, emailId, null);
            return emailId;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("appid", specAccount[0]);
        params.put("signature", specAccount[1]);
        params.put("to", to);
        params.put("from", specAccount[2]);
        params.put("from_name", specAccount[3]);
        params.put("subject", subject);
        if (useTemplate) {
            params.put("html", content);
        } else {
            params.put("text", content);
        }
        params.put("asynchronous", "true");

        JSONObject rJson;
        try {
            String r = HttpUtils.post("https://api.mysubmail.com/mail/send.json", params);
            rJson = JSON.parseObject(r);
        } catch (Exception ex) {
            log.error("Mail failed to send : " + to + " > " + subject, ex);
            return null;
        }

        JSONArray returns = rJson.getJSONArray("return");
        if (STATUS_OK.equalsIgnoreCase(rJson.getString("status")) && !returns.isEmpty()) {
            String sendId = ((JSONObject) returns.get(0)).getString("send_id");
            createLog(to, logContent, TYPE_EMAIL, sendId, null);
            return sendId;

        } else {
            log.error("Mail failed to send : " + to + " > " + subject + "\nError : " + rJson);
            createLog(to, logContent, TYPE_EMAIL, null, rJson.getString("msg"));
            return null;
        }
    }

    /**
     * SMTP 发送
     *
     * @param to
     * @param subject
     * @param htmlContent
     * @param specAccount
     * @return
     * @throws ConfigurationException
     */
    private static String sendMailViaSmtp(String to, String subject, String htmlContent, String[] specAccount) throws EmailException {
        HtmlEmail email = new HtmlEmail();
        email.addTo(to);
        email.setSubject(subject);
        email.setHtmlMsg(htmlContent);

        email.setAuthentication(specAccount[0], specAccount[1]);
        email.setFrom(specAccount[2], specAccount[3]);

        // host:port:ssl
        String[] hostPortSsl = specAccount[4].split(":");
        email.setHostName(hostPortSsl[0]);
        if (hostPortSsl.length > 1) email.setSmtpPort(Integer.parseInt(hostPortSsl[1]));
        if (hostPortSsl.length > 2) email.setSSLOnConnect("ssl".equalsIgnoreCase(hostPortSsl[2]));

        email.setCharset("UTF-8");
        return email.send();
    }

    private static Element MT_CACHE = null;
    /**
     * @return
     */
    protected static Element getMailTemplate() {
        if (MT_CACHE != null && !Application.devMode()) return MT_CACHE.clone();

        String content = CommonsUtils.getStringOfRes("i18n/email.zh_CN.html");
        Assert.notNull(content, "Cannot load template of email");

        // 生硬替换
        if (Application.isReady() && License.getCommercialType() > 10) {
            content = content.replace("REBUILD", RebuildConfiguration.get(ConfigurationItem.AppName));
            content = content.replace("https://getrebuild.com/img/logo.png", RebuildConfiguration.getHomeUrl("commons/theme/use-logo"));
            content = content.replace("https://getrebuild.com/", RebuildConfiguration.getHomeUrl());
        }

        Document html = Jsoup.parse(content);
        MT_CACHE = html.body();

        return MT_CACHE.clone();
    }

    /**
     * @param to
     * @param content
     */
    public static void sendSMSAsync(String to, String content) {
        ThreadPool.exec(() -> {
            try {
                sendSMS(to, content);
            } catch (Exception ex) {
                log.error("SMS failed to send : " + to, ex);
            }
        });
    }

    /**
     * @param to
     * @param content
     * @return
     * @throws ConfigurationException
     */
    public static String sendSMS(String to, String content) throws ConfigurationException {
        return sendSMS(to, content, RebuildConfiguration.getSmsAccount());
    }

    /**
     * @param to
     * @param content
     * @param specAccount
     * @return <tt>null</tt> if failed or SENDID
     * @throws ConfigurationException If sms-account unset
     */
    public static String sendSMS(String to, String content, String[] specAccount) throws ConfigurationException {
        if (specAccount == null || specAccount.length < 3
                || StringUtils.isBlank(specAccount[0]) || StringUtils.isBlank(specAccount[1])
                || StringUtils.isBlank(specAccount[2])) {
            throw new ConfigurationException(Language.L("短信账户未配置或配置错误"));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("appid", specAccount[0]);
        params.put("signature", specAccount[1]);
        params.put("to", to);
        // Auto appended the SMS-Sign (China only?)
        if (!content.startsWith("【")) {
            content = "【" + specAccount[2] + "】" + content;
        }
        params.put("content", content);

        JSONObject rJson;
        try {
            String r = HttpUtils.post("https://api.mysubmail.com/message/send.json", params);
            rJson = JSON.parseObject(r);
        } catch (Exception ex) {
            log.error("SMS failed to send : " + to + " > " + content, ex);
            return null;
        }

        if (STATUS_OK.equalsIgnoreCase(rJson.getString("status"))) {
            String sendId = rJson.getString("send_id");
            createLog(to, content, TYPE_SMS, sendId, null);
            return sendId;

        } else {
            log.error("SMS failed to send : " + to + " > " + content + "\nError : " + rJson);
            createLog(to, content, TYPE_SMS, null, rJson.getString("msg"));
            return null;
        }
    }

    /**
     * 记录发送日志
     *
     * @param to
     * @param content
     * @param type    1=短信 2=邮件
     * @param sentid
     * @param error
     */
    private static void createLog(String to, String content, int type, String sentid, String error) {
        if (!Application.isReady()) return;

        Record log = EntityHelper.forNew(EntityHelper.SmsendLog, UserService.SYSTEM_USER);
        log.setString("to", to);
        log.setString("content", CommonsUtils.maxstr(content, 10000));
        log.setDate("sendTime", CalendarUtils.now());
        log.setInt("type", type);
        if (sentid != null) {
            log.setString("sendResult", sentid);
        } else {
            log.setString("sendResult",
                    CommonsUtils.maxstr("ERR:" + StringUtils.defaultIfBlank(error, "Unknow"), 200));
        }
        Application.getCommonsService().create(log);
    }

    /**
     * 短信服务可用
     *
     * @return
     */
    public static boolean availableSMS() {
        return RebuildConfiguration.getSmsAccount() != null;
    }

    /**
     * 邮件服务可用
     *
     * @return
     */
    public static boolean availableMail() {
        return RebuildConfiguration.getMailAccount() != null;
    }
}
