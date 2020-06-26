/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * SUBMAIL SMS/MAIL 发送类
 * 
 * @author devezhao
 * @since 01/03/2019
 */
public class SMSender {
	
	private static final Log LOG = LogFactory.getLog(SMSender.class);

	private static final String STATUS_OK = "success";

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
				LOG.error("Mail failed to send : " + to + " < " + subject, ex);
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
		return sendMail(to, subject, content, true, SysConfiguration.getMailAccount());
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
            throw new ConfigurationException("邮箱账户未配置/无效");
        }

		Map<String, Object> params = new HashMap<>();
		params.put("appid", specAccount[0]);
		params.put("signature", specAccount[1]);
		params.put("to", to);
		params.put("from", specAccount[2]);
		params.put("from_name", specAccount[3]);
		params.put("subject", subject);
		if (useTemplate) {
			Element mailbody;
			try {
				mailbody = getMailTemplate();
			} catch (IOException e) {
				LOG.error("Couldn't load template of mail", e);
				return null;
			}
			
			mailbody.selectFirst(".rb-title").text(subject);
			mailbody.selectFirst(".rb-content").html(content);
			String htmlContent = mailbody.html();
			// 处理公共变量
			htmlContent = htmlContent.replace("%TO%", to);
			htmlContent = htmlContent.replace("%TIME%", CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now()));
			htmlContent = htmlContent.replace("%APPNAME%", SysConfiguration.get(ConfigurableItem.AppName));

			params.put("html", htmlContent);
			content = htmlContent;
		} else {
			params.put("text", content);
		}
		params.put("asynchronous", "true");

		JSONObject rJson;
		try {
			String r = CommonsUtils.post("https://api.mysubmail.com/mail/send.json", params);
			rJson = JSON.parseObject(r);
		} catch (Exception ex) {
			LOG.error("Mail failed to send : " + to + " > " + subject, ex);
			return null;
		}

		final String scontent = "【" + subject + "】" + content;

		JSONArray returns = rJson.getJSONArray("return");
		if (STATUS_OK.equalsIgnoreCase(rJson.getString("status")) && !returns.isEmpty()) {
			String sendId = ((JSONObject) returns.get(0)).getString("send_id");
			createLog(to, scontent, 2, sendId, null);
			return sendId;

		} else {
			LOG.error("Mail failed to send : " + to + " > " + subject + "\nError : " + rJson);

			createLog(to, scontent, 2, null, rJson.getString("msg"));
			return null;
		}
	}
	
	/**
	 * @return
	 * @throws IOException
	 */
	protected static Element getMailTemplate() throws IOException {
		File tmp = SysConfiguration.getFileOfRes("locales/email_zh-CN.html");
		Document html = Jsoup.parse(tmp, "utf-8");
		return html.body();
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
				LOG.error("SMS failed to send : " + to, ex);
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
		return sendSMS(to, content, SysConfiguration.getSmsAccount());
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
            throw new ConfigurationException("短信账户未配置/无效");
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
			String r = CommonsUtils.post("https://api.mysubmail.com/message/send.json", params);
			rJson = JSON.parseObject(r);
		} catch (Exception ex) {
			LOG.error("SMS failed to send : " + to + " > " + content, ex);
			return null;
		}
		
		if (STATUS_OK.equalsIgnoreCase(rJson.getString("status"))) {
			String sendId = rJson.getString("send_id");
			createLog(to, content, 1, sendId, null);
			return sendId;

		} else {
			LOG.error("SMS failed to send : " + to + " > " + content + "\nError : " + rJson);

			createLog(to, content, 1, null, rJson.getString("msg"));
			return null;
		}
	}

	/**
	 * 发送日志
	 *
	 * @param to
	 * @param content
	 * @param type 1=短信 2=邮件
	 * @param sentid
	 * @param error
	 */
	private static void createLog(String to, String content, int type, String sentid, String error) {
		if (!Application.serversReady()) return;

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
		Application.getCommonService().create(log);
	}
	
	/**
     * 短信服务可用
     *
	 * @return
	 */
	public static boolean availableSMS() {
		return SysConfiguration.getSmsAccount() != null;
	}
	
	/**
     * 邮件服务可用
     *
	 * @return
	 */
	public static boolean availableMail() {
		return SysConfiguration.getMailAccount() != null;
	}
}
