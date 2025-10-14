/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.integration;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.Callback2;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.HeavyStopWatcher;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.OkHttpUtils;
import com.rebuild.utils.md.MarkdownUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
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

    // -- 邮件

    /**
     * @param to
     * @param subject
     * @param content
     */
    public static void sendMailAsync(String to, String subject, String content) {
        sendMailAsync(to, subject, content, null, null);
    }

    /**
     * @param to
     * @param subject
     * @param content
     * @param attach
     */
    public static void sendMailAsync(String to, String subject, String content, File[] attach) {
        sendMailAsync(to, subject, content, attach, null);
    }

    /**
     * @param to
     * @param subject
     * @param content
     * @param attach
     * @param cb
     */
    public static void sendMailAsync(String to, String subject, String content, File[] attach, Callback2 cb) {
        ThreadPool.exec(() -> {
            try {
                String sendid = sendMail(to, subject, content, attach);
                if (cb != null) cb.onComplete(sendid);

            } catch (Exception ex) {
                log.error("Email send error : {}, {}", to, subject, ex);
                if (cb != null) cb.onComplete(ex);
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
        return sendMail(to, subject, content, null);
    }

    /**
     * @param to
     * @param subject
     * @param content
     * @param attach
     * @return
     */
    public static String sendMail(String to, String subject, String content, File[] attach) {
        return sendMail(to, subject, content, attach, true, RebuildConfiguration.getMailAccount());
    }

    /**
     * @param to
     * @param subject
     * @param content
     * @param attach
     * @param useTemplate
     * @param specAccount
     * @return
     * @throws ConfigurationException
     */
    public static String sendMail(String to, String subject, String content, File[] attach, boolean useTemplate, String[] specAccount) throws ConfigurationException {
        if (specAccount == null || specAccount.length < 6
                || StringUtils.isBlank(specAccount[0]) || StringUtils.isBlank(specAccount[1])
                || StringUtils.isBlank(specAccount[2]) || StringUtils.isBlank(specAccount[3])) {
            throw new ConfigurationException(Language.L("邮件账户未配置或配置错误"));
        }

        if (Application.devMode()) {
            log.info("[dev] FAKE SEND EMAIL. T:{}, S:{}, M:{}, F:{}", to, subject, content, attach);
            return null;
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
            if (License.isRbvAttached()) {
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
        if (specAccount.length >= 7 && StringUtils.isNotBlank(specAccount[6])) {
            try {
                String sendid = sendMailViaSmtp(to, subject, content, attach, specAccount);
                createLog(to, logContent, TYPE_EMAIL, sendid, null);
                return sendid;

            } catch (EmailException ex) {
                log.error("SMTP send error : {}, {}, {}", to, subject, content, ex);
                createLog(to, logContent, TYPE_EMAIL, null, ex);
                return null;
            }
        }

        Map<String, Object> params = new HashMap<>();
        params.put("appid", specAccount[0]);
        params.put("signature", specAccount[1]);
        params.put("to", to);
        if (StringUtils.isNotBlank(specAccount[4])) params.put("cc", specAccount[4]);
        if (StringUtils.isNotBlank(specAccount[5])) params.put("bcc", specAccount[5]);
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

        // v3.7 附件
        if (attach != null) {
            JSONArray atta = new JSONArray();
            for (File a : attach) {
                String base64;
                try {
                    byte[] bs = FileUtils.readFileToByteArray(a);
                    base64 = Base64.getEncoder().encodeToString(bs);
                } catch (IOException ex) {
                    continue;
                }

                Map<String, String> map = new HashMap<>(2);
                map.put("name", a.getName());
                map.put("data", base64);
                atta.add(map);
            }
            if (!atta.isEmpty()) params.put("atta", atta);
        }

        JSONObject rJson;
        try {
            String r = OkHttpUtils.post("https://api-v4.mysubmail.com/mail/send.json", params);
            rJson = JSON.parseObject(r);
        } catch (Exception ex) {
            log.error("Submail send error : {}, {}, {}", to, subject, content, ex);
            createLog(to, logContent, TYPE_EMAIL, null, ex);
            return null;
        }

        JSONArray returns = rJson.getJSONArray("return");
        if (STATUS_OK.equalsIgnoreCase(rJson.getString("status")) && !returns.isEmpty()) {
            String sendId = ((JSONObject) returns.get(0)).getString("send_id");
            createLog(to, logContent, TYPE_EMAIL, sendId, null);
            return sendId;
        }

        log.error("Submail send fails : {}, {}, {}\nERROR : {}", to, subject, content, rJson);
        createLog(to, logContent, TYPE_EMAIL, null, rJson.getString("msg"));
        return null;
    }

    /**
     * SMTP 发送
     *
     * @param to
     * @param subject
     * @param htmlContent
     * @param attach
     * @param specAccount
     * @return
     * @throws ConfigurationException
     */
    protected static String sendMailViaSmtp(String to, String subject, String htmlContent, File[] attach, String[] specAccount) throws EmailException {
        HtmlEmail email = new HtmlEmail();
        // v4.1 多个
        for (String o : to.split(",")) {
            email.addTo(o);
        }
        if (StringUtils.isNotBlank(specAccount[4])) email.addCc(specAccount[4]);
        if (StringUtils.isNotBlank(specAccount[5])) email.addBcc(specAccount[5]);
        email.setSubject(subject);
        email.setHtmlMsg(htmlContent);

        email.setFrom(specAccount[2], specAccount[3]);
        email.setAuthentication(specAccount[0], specAccount[1]);

        // HOST[:PORT:SSL|TLS]
        String[] hostPortSsl = specAccount[6].split(":");
        email.setHostName(hostPortSsl[0]);
        if (hostPortSsl.length > 1) email.setSmtpPort(Integer.parseInt(hostPortSsl[1]));
        if (hostPortSsl.length > 2) {
            if ("ssl".equalsIgnoreCase(hostPortSsl[2])) {
                email.setSSLOnConnect(true);
            } else if ("tls".equalsIgnoreCase(hostPortSsl[2])) {
                email.setStartTLSEnabled(true);
                email.setStartTLSRequired(true);
            }
        }

        if (attach != null) {
            for (File a : attach) email.attach(a);
        }

        email.addHeader("X-User-Agent", OkHttpUtils.RB_UA);
        email.setCharset(AppUtils.UTF8);
        email.setSocketTimeout(EmailConstants.SOCKET_TIMEOUT_MS * 2);
        email.setSocketConnectionTimeout(EmailConstants.SOCKET_TIMEOUT_MS * 2);
        return email.send();
    }

    private static Element MT_CACHE = null;
    /**
     * @return
     */
    protected static Element getMailTemplate() {
        if (Application.devMode()) MT_CACHE = null;
        if (MT_CACHE != null) return MT_CACHE.clone();

        String content = null;
        // v3.9.3 从数据目录
        File file = RebuildConfiguration.getFileOfData("email.zh_CN.html");
        if (file.exists()) {
            try {
                content = FileUtils.readFileToString(file, AppUtils.UTF8);
            } catch (IOException ex) {
                log.warn("Cannot read file of email template : {}", file, ex);
            }
        }
        if (content == null) {
            content = CommonsUtils.getStringOfRes("i18n/email.zh_CN.html");
        }
        Assert.notNull(content, "Cannot read email template");

        Document html = Jsoup.parse(content);
        MT_CACHE = html.body();
        return MT_CACHE.clone();
    }

    // -- 短信

    /**
     * @param to
     * @param content
     */
    public static void sendSMSAsync(String to, String content) {
        sendSMSAsync(to, content, null);
    }

    /**
     * @param to
     * @param content
     * @param cb
     */
    public static void sendSMSAsync(String to, String content, Callback2 cb) {
        ThreadPool.exec(() -> {
            try {
                String sendid = sendSMS(to, content);
                if (cb != null) cb.onComplete(sendid);

            } catch (Exception ex) {
                log.error("SMS send error : {}, {}", to, content, ex);
                if (cb != null) cb.onComplete(ex);
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
     * @return null if failed or SENDID
     * @throws ConfigurationException If sms-account unset
     */
    public static String sendSMS(String to, String content, String[] specAccount) throws ConfigurationException {
        if (specAccount == null || specAccount.length < 3
                || StringUtils.isBlank(specAccount[0]) || StringUtils.isBlank(specAccount[1])
                || StringUtils.isBlank(specAccount[2])) {
            throw new ConfigurationException(Language.L("短信账户未配置或配置错误"));
        }

        if (Application.devMode()) {
            log.warn("[dev] FAKE SEND SMS. T:{}, M:{}", to, content);
            return null;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("appid", specAccount[0]);
        params.put("signature", specAccount[1]);
        params.put("to", to);
        // 短信签名
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
            log.error("Subsms send error : {}, {}", to, content, ex);
            createLog(to, content, TYPE_SMS, null, ex);
            return null;
        } finally {
            HeavyStopWatcher.clean();
        }

        if (STATUS_OK.equalsIgnoreCase(rJson.getString("status"))) {
            String sendId = rJson.getString("send_id");
            createLog(to, content, TYPE_SMS, sendId, null);
            return sendId;
        }

        log.error("Subsms send fails : {}, {}\nERROR : {}", to, content, rJson);
        createLog(to, content, TYPE_SMS, null, rJson.getString("msg"));
        return null;
    }

    // -- SUPPORTS

    // @see com.rebuild.core.support.CommonsLog
    private static void createLog(String to, String content, int type, String sentid, Object error) {
        if (!Application.isReady()) return;

        Record slog = EntityHelper.forNew(EntityHelper.SmsendLog, UserService.SYSTEM_USER);
        slog.setString("to", CommonsUtils.maxstr(to, 700));
        slog.setString("content", CommonsUtils.maxstr(content, 10000));
        slog.setDate("sendTime", CalendarUtils.now());
        slog.setInt("type", type);
        if (sentid != null) {
            slog.setString("sendResult", sentid);
        } else {
            String errorMsg = null;
            if (error instanceof Exception) {
                errorMsg = ThrowableUtils.getRootCause((Exception) error).getLocalizedMessage();
            }
            if (errorMsg == null) errorMsg = "Unknow";
            else errorMsg = CommonsUtils.maxstr(errorMsg, 200);
            slog.setString("sendResult", "ERR:" + errorMsg);
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
