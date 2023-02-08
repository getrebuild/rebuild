/*!
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
import com.rebuild.core.support.HeavyStopWatcher;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.*;
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
                log.error("Email failed to send!", ex);
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
     * @param specAccount
     * @return <tt>null</tt> if failed or SENDID
     * @throws ConfigurationException If mail-account unset
     */
    public static String sendMail(String to, String subject, String content, boolean useTemplate, String[] specAccount) throws ConfigurationException {
        if (specAccount == null || specAccount.length < 5
                || StringUtils.isBlank(specAccount[0]) || StringUtils.isBlank(specAccount[1])
                || StringUtils.isBlank(specAccount[2]) || StringUtils.isBlank(specAccount[3])) {
            throw new ConfigurationException(Language.L("邮件账户未配置或配置错误"));
        }

        // 使用邮件模板
        if (useTemplate) {
            Element mailbody = getMailTemplate();

            Objects.requireNonNull(mailbody.selectFirst(".rb-title")).text(subject);
            Objects.requireNonNull(mailbody.selectFirst(".rb-content")).html(content);

            // 处理变量
            String htmlContent = mailbody.html();
            htmlContent = htmlContent.replace("%TO%", to);
            htmlContent = htmlContent.replace("%TIME%", CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now()));
            htmlContent = htmlContent.replace("%APPURL%", RebuildConfiguration.getHomeUrl());
            htmlContent = htmlContent.replace("%APPLOGO%", RebuildConfiguration.getHomeUrl("commons/theme/use-logo"));
            if (License.isCommercial()) {
                htmlContent = htmlContent.replace("%APPNAME%", RebuildConfiguration.get(ConfigurationItem.AppName));
            } else {
                htmlContent = htmlContent.replace("%APPNAME%", "REBUILD");
            }

            String pageFooter = RebuildConfiguration.get(ConfigurationItem.PageFooter);
            if (StringUtils.isNotBlank(pageFooter)) {
                pageFooter = MarkdownUtils.render(pageFooter);
                htmlContent = htmlContent.replace("%PAGE_FOOTER%", pageFooter);
            } else {
                htmlContent = htmlContent.replace("%PAGE_FOOTER%", "");
            }

            content = htmlContent;
        }

        final String logContent = "【" + subject + "】" + content;

        // Use SMTP
        if (specAccount.length >= 6 && StringUtils.isNotBlank(specAccount[5])) {
            try {
                String emailId = sendMailViaSmtp(to, subject, content, specAccount);
                createLog(to, logContent, TYPE_EMAIL, emailId, null);
                return emailId;

            } catch (EmailException ex) {
                log.error("SMTP failed to send : {} | {} | {}", to, subject, content, ex);
                return null;
            }
        }

        Map<String, Object> params = new HashMap<>();
        params.put("appid", specAccount[0]);
        params.put("signature", specAccount[1]);
        params.put("to", to);
        if (StringUtils.isNotBlank(specAccount[4])) params.put("cc", specAccount[4]);
        params.put("from", specAccount[2]);
        params.put("from_name", specAccount[3]);
        params.put("subject", subject);
        if (useTemplate) {
            params.put("html", content);
        } else {
            params.put("text", content);
        }
        params.put("asynchronous", "true");
        params.put("headers", JSONUtils.toJSONObject("X-User-Agent", OkHttpUtils.RB_UA));

        JSONObject rJson;
        try {
            String r = OkHttpUtils.post("https://api-v4.mysubmail.com/mail/send.json", params);
            rJson = JSON.parseObject(r);
        } catch (Exception ex) {
            log.error("Submail failed to send : {} | {} | {}", to, subject, content, ex);
            return null;
        }

        JSONArray returns = rJson.getJSONArray("return");
        if (STATUS_OK.equalsIgnoreCase(rJson.getString("status")) && !returns.isEmpty()) {
            String sendId = ((JSONObject) returns.get(0)).getString("send_id");
            createLog(to, logContent, TYPE_EMAIL, sendId, null);
            return sendId;
        }

        log.error("Submail failed to send : {} | {} | {}\nError : {}", to, subject, content, rJson);
        createLog(to, logContent, TYPE_EMAIL, null, rJson.getString("msg"));
        return null;
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
    protected static String sendMailViaSmtp(String to, String subject, String htmlContent, String[] specAccount) throws EmailException {
        HtmlEmail email = new HtmlEmail();
        email.addTo(to);
        if (StringUtils.isNotBlank(specAccount[4])) email.addCc(specAccount[4]);
        email.setSubject(subject);
        email.setHtmlMsg(htmlContent);

        email.setFrom(specAccount[2], specAccount[3]);
        email.setAuthentication(specAccount[0], specAccount[1]);

        // HOST[:PORT:SSL]
        String[] hostPortSsl = specAccount[5].split(":");
        email.setHostName(hostPortSsl[0]);
        if (hostPortSsl.length > 1) email.setSmtpPort(Integer.parseInt(hostPortSsl[1]));
        if (hostPortSsl.length > 2) email.setSSLOnConnect("ssl".equalsIgnoreCase(hostPortSsl[2]));

        email.addHeader("X-User-Agent", OkHttpUtils.RB_UA);
        email.setCharset(AppUtils.UTF8);
        return email.send();
    }

    private static Element MT_CACHE = null;
    /**
     * @return
     */
    protected static Element getMailTemplate() {
        if (MT_CACHE != null) return MT_CACHE.clone();

        String content = CommonsUtils.getStringOfRes("i18n/email.zh_CN.html");
        Assert.notNull(content, "Cannot load template of email");
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
                log.error("SMS failed to send!", ex);
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
        // Auto append the SMS-Sign
        if (!content.startsWith("【")) {
            content = "【" + specAccount[2] + "】" + content;
        }
        params.put("content", content);

        HeavyStopWatcher.createWatcher("Subsms Send", to);
        JSONObject rJson;
        try {
            String r = OkHttpUtils.post("https://api-v4.mysubmail.com/sms/send.json", params);
            rJson = JSON.parseObject(r);
        } catch (Exception ex) {
            log.error("Subsms failed to send : {} | {}", to, content, ex);
            return null;
        } finally {
            HeavyStopWatcher.clean();
        }

        if (STATUS_OK.equalsIgnoreCase(rJson.getString("status"))) {
            String sendId = rJson.getString("send_id");
            createLog(to, content, TYPE_SMS, sendId, null);
            return sendId;
        }

        log.error("Subsms failed to send : {} | {}\nError : {}", to, content, rJson);
        createLog(to, content, TYPE_SMS, null, rJson.getString("msg"));
        return null;
    }

    // @see com.rebuild.core.support.CommonsLog
    private static void createLog(String to, String content, int type, String sentid, String error) {
        if (!Application.isReady()) return;

        Record slog = EntityHelper.forNew(EntityHelper.SmsendLog, UserService.SYSTEM_USER);
        slog.setString("to", to);
        slog.setString("content", CommonsUtils.maxstr(content, 10000));
        slog.setDate("sendTime", CalendarUtils.now());
        slog.setInt("type", type);
        if (sentid != null) {
            slog.setString("sendResult", sentid);
        } else {
            slog.setString("sendResult",
                    CommonsUtils.maxstr("ERR:" + StringUtils.defaultIfBlank(error, "Unknow"), 200));
        }

        Application.getCommonsService().create(slog);
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
