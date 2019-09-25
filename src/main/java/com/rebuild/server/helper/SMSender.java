/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper;

import cn.devezhao.commons.ThreadPool;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.Assert;

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

	/**
	 * @param to
	 * @param subject
	 * @param content
	 * @return
	 */
	public static String sendMail(String to, String subject, String content) {
		return sendMail(to, subject, content, true);
	}
	
	/**
	 * @param to
	 * @param subject
	 * @param content
	 */
	public static void sendMailAsync(String to, String subject, String content) {
		ThreadPool.exec(() -> {
			try {
				sendMail(to, subject, content, true);
			} catch (Exception ex) {
				LOG.error("Mail send failure: " + to + " < " + subject, ex);
			}
		});
	}
	
	/**
	 * @param to
	 * @param subject
	 * @param content
	 * @param useTemplate
	 * @return <tt>null</tt> if failed or SENDID
	 * @throws ConfigurationException If mail-account unset
	 */
	public static String sendMail(String to, String subject, String content, boolean useTemplate) throws ConfigurationException {
		String account[] = SysConfiguration.getMailAccount();
		Assert.notNull(account, "邮箱账户未配置");
		
		Map<String, Object> params = new HashMap<>();
		params.put("appid", account[0]);
		params.put("signature", account[1]);
		params.put("to", to);
		params.put("from", account[2]);
		params.put("from_name", account[3]);
		params.put("subject", subject);
		if (useTemplate) {
			Element mailbody = null;
			try {
				mailbody = getMailTemplate();
			} catch (IOException e) {
				LOG.error("Couldn't load mail template", e);
				return null;
			}
			
			mailbody.selectFirst(".rb-title").text(subject);
			mailbody.selectFirst(".rb-content").html(content);
			String eHTML = mailbody.html();
			// 处理变量
			eHTML = eHTML.replace("%TO%", to);

			params.put("html", eHTML);
		} else {
			params.put("text", content);
		}
		params.put("asynchronous", "true");

		JSONObject rJson = null;
		try {
			String r = CommonsUtils.post("https://api.mysubmail.com/mail/send.json", params);
			rJson = JSON.parseObject(r);
		} catch (Exception ex) {
			LOG.error("Mail failed : " + to + " > " + subject, ex);
			return null;
		}

		if ("success".equals(rJson.getString("status"))) {
			JSONArray returns = rJson.getJSONArray("return");
			if (returns.isEmpty()) {
				LOG.error("Mail failed : " + to + " > " + subject + "\nError : " + rJson);
				return null;
			}
			return ((JSONObject) returns.get(0)).getString("send_id");
		} else {
			LOG.error("Mail failed : " + to + " > " + subject + "\nError : " + rJson);
		}
		return null;
	}
	
	/**
	 * @return
	 * @throws IOException
	 */
	protected static Element getMailTemplate() throws IOException {
		File temp = SysConfiguration.getFileOfRes("locales/mail-notify.html");
		Document html = Jsoup.parse(temp, "utf-8");
		return html.body();
	}
	
	/**
	 * @param to
	 * @param content
	 * @return <tt>null</tt> if failed or SENDID
	 * @throws ConfigurationException If sms-account unset
	 */
	public static String sendSMS(String to, String content) throws ConfigurationException {
		String account[] = SysConfiguration.getSmsAccount();
		Assert.notNull(account, "短信账户未配置");
		
		Map<String, Object> params = new HashMap<>();
		params.put("appid", account[0]);
		params.put("signature", account[1]);
		params.put("to", to);
		// Auto appended the SMS-Sign (China only?)
		if (!content.startsWith("【")) {
			content = "【" + account[2] + "】" + content;
		}
		params.put("content", content);

		JSONObject rJson = null;
		try {
			String r = CommonsUtils.post("https://api.mysubmail.com/message/send.json", params);
			rJson = JSON.parseObject(r);
		} catch (Exception ex) {
			LOG.error("SMS failed : " + to + " > " + content, ex);
			return null;
		}
		
		if ("success".equals(rJson.getString("status"))) {
			return rJson.getString("send_id");
		} else {
			LOG.error("SMS failed : " + to + " > " + content + "\nError : " + rJson);
		}
		return null;
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
				LOG.error("SMS send failure: " + to, ex);
			}
		});
	}
	
	/**
	 * @return
	 */
	public static boolean availableSMS() {
		return SysConfiguration.getSmsAccount() != null;
	}
	
	/**
	 * @return
	 */
	public static boolean availableMail() {
		return SysConfiguration.getMailAccount() != null;
	}
}
