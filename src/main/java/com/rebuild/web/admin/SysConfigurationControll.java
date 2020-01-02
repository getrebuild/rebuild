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

package com.rebuild.web.admin;

import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONObject;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.util.Auth;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.Lisence;
import com.rebuild.server.helper.QiniuCloud;
import com.rebuild.server.helper.SMSender;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.web.BasePageControll;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 系统配置
 * 
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 * @see SysConfiguration
 */
@Controller
@RequestMapping("/admin/")
public class SysConfigurationControll extends BasePageControll {

	@RequestMapping("systems")
	public ModelAndView pageSystems() {
		ModelAndView mv = createModelAndView("/admin/system-general.jsp");
		for (ConfigurableItem item : ConfigurableItem.values()) {
			mv.getModel().put(item.name(), SysConfiguration.get(item));
		}
		return mv;
	}

    @RequestMapping(value = "systems", method = RequestMethod.POST)
    public void postSystems(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        String dHomeURL = defaultIfBlank(data, ConfigurableItem.HomeURL);
        if (!RegexUtils.isUrl(dHomeURL)) {
            writeFailure(response, "无效主页地址/域名");
            return;
        }
        String dRecycleBinKeepingDays = defaultIfBlank(data, ConfigurableItem.RecycleBinKeepingDays);
        if (!NumberUtils.isNumber(dRecycleBinKeepingDays)) {
            data.put(ConfigurableItem.RecycleBinKeepingDays.name(), ConfigurableItem.RecycleBinKeepingDays.getDefaultValue());
        }

        setValues(data);

        // @see ServerListener
        request.getServletContext().setAttribute("appName", SysConfiguration.get(ConfigurableItem.AppName));
        request.getServletContext().setAttribute("homeUrl", SysConfiguration.get(ConfigurableItem.HomeURL));

        writeSuccess(response);
    }
	
	@RequestMapping("integration/storage")
	public ModelAndView pageIntegrationStorage() {
		ModelAndView mv = createModelAndView("/admin/integration/storage-qiniu.jsp");
		mv.getModel().put("storageAccount",
				starsAccount(SysConfiguration.getStorageAccount(), 0, 1));
		mv.getModel().put("storageStatus", QiniuCloud.instance().available());
		return mv;
	}

    @RequestMapping(value = "integration/storage", method = RequestMethod.POST)
    public void postIntegrationStorage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        String dStorageURL = defaultIfBlank(data, ConfigurableItem.StorageURL);
        String dStorageBucket = defaultIfBlank(data, ConfigurableItem.StorageBucket);
        String dStorageApiKey = defaultIfBlank(data, ConfigurableItem.StorageApiKey);
        String dStorageApiSecret = defaultIfBlank(data, ConfigurableItem.StorageApiSecret);

        if (dStorageURL.startsWith("//")) {
            dStorageURL = "https:" + dStorageURL;
        }
        if (!RegexUtils.isUrl(dStorageURL)) {
            writeFailure(response, "无效访问域名");
            return;
        }

        try {
            // Test
            Auth auth = Auth.create(dStorageApiKey, dStorageApiSecret);
            BucketManager bucketManager = new BucketManager(auth, QiniuCloud.CONFIGURATION);
            bucketManager.getBucketInfo(dStorageBucket);

            setValues(data);
            writeSuccess(response);

        } catch (QiniuException ex) {
            writeFailure(response, "无效配置参数 : " + ex.response.error);
        } catch (Exception ex) {
            writeFailure(response, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
        }
    }
	
	@RequestMapping("integration/submail")
	public ModelAndView pageIntegrationSubmail() {
		ModelAndView mv = createModelAndView("/admin/integration/submail.jsp");
		mv.getModel().put("smsAccount",
				starsAccount(SysConfiguration.getSmsAccount(), 1));
		mv.getModel().put("mailAccount", 
				starsAccount(SysConfiguration.getMailAccount(), 1));
		return mv;
	}

    @RequestMapping(value = "integration/submail", method = RequestMethod.POST)
    public void postIntegrationSubmail(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        String dMailAddr = defaultIfBlank(data, ConfigurableItem.MailAddr);
        if (!RegexUtils.isEMail(dMailAddr)) {
            writeFailure(response, "无效发件人地址");
            return;
        }

        setValues(data);
        writeSuccess(response);
    }

    @RequestMapping(value = "integration/submail/test", method = RequestMethod.POST)
    public void testSubmail(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);
	    String type = getParameterNotNull(request, "type");
	    String receiver = getParameterNotNull(request, "receiver");

	    String sent = null;
	    if ("SMS".equalsIgnoreCase(type)) {
	        if (!RegexUtils.isCNMobile(receiver)) {
                writeFailure(response, "无效接收手机");
                return;
            }

	        String[] specAccount = new String[] {
                    data.getString("SmsUser"), data.getString("SmsPassword"),
                    data.getString("SmsSign")
            };
	        if (specAccount[1].contains("**********")) specAccount[1] = SysConfiguration.get(ConfigurableItem.SmsPassword);

            sent = SMSender.sendSMS(receiver, "收到此消息说明你的短信服务配置正确", specAccount);
        } else if ("EMAIL".equalsIgnoreCase(type)) {
            if (!RegexUtils.isEMail(receiver)) {
                writeFailure(response, "无效接收邮箱");
                return;
            }

            String[] specAccount = new String[] {
                    data.getString("MailUser"), data.getString("MailPassword"),
                    data.getString("MailAddr"), data.getString("MailName")
            };
            if (specAccount[1].contains("**********")) specAccount[1] = SysConfiguration.get(ConfigurableItem.MailPassword);

            sent = SMSender.sendMail(receiver, "测试邮件", "收到此消息说明你的邮件服务配置正确", true, specAccount);
        }

	    if (sent != null) {
	        writeSuccess(response, sent);
        } else {
            writeFailure(response, "测试发送失败，请检查你的配置");
        }
    }

	@RequestMapping("systems/query-authority")
	public void queryAuthority(HttpServletResponse response) throws IOException {
		writeSuccess(response, Lisence.queryAuthority());
	}

	private String[] starsAccount(String[] account, int ...index) {
		if (account == null) {
			return null;
		}
		for (int i : index) {
			account[i] = CommonsUtils.stars(account[i]);
		}
		return account;
	}

	private String defaultIfBlank(JSONObject data, ConfigurableItem item) {
	    return StringUtils.defaultIfBlank(data.getString(item.name()), SysConfiguration.get(item));
    }

    private void setValues(JSONObject data) {
        for (Map.Entry<String, Object> e : data.entrySet()) {
            try {
                ConfigurableItem item = ConfigurableItem.valueOf(e.getKey());
                SysConfiguration.set(item, e.getValue());
            } catch (Exception ex) {
                LOG.error("Invalid item : " + e.getKey() + " = " + e.getValue());
            }
        }
    }
}
