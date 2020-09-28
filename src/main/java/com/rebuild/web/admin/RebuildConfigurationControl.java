/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONObject;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.util.Auth;
import com.rebuild.core.Application;
import com.rebuild.core.support.*;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.RebuildWebConfigurer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 系统配置
 *
 * @author zhaofang123@gmail.com
 * @see RebuildConfiguration
 * @see ConfigurationItem
 * @since 09/20/2018
 */
@Controller
@RequestMapping("/admin/")
public class RebuildConfigurationControl extends BaseController {

    @GetMapping("systems")
    public ModelAndView pageSystems() {
        ModelAndView mv = createModelAndView("/admin/system-cfg");
        for (ConfigurationItem item : ConfigurationItem.values()) {
            mv.getModel().put(item.name(), RebuildConfiguration.get(item));
        }

        // Available lang
        JSONObject langsJson = new JSONObject();
        for (String locale : Application.getLanguage().availableLocales()) {
            Locale inst = Locale.forLanguageTag(locale.split("[_-]")[0]);
            langsJson.put(locale, inst.getDisplayName(inst) + " (" + locale + ")");
        }
        mv.getModel().put("availableLangs", langsJson);

        JSONObject auth = License.queryAuthority(false);
        mv.getModel().put("LicenseType",
                auth.getString("authType") + " (" + auth.getString("authObject") + ")");
        mv.getModel().put("Version", Application.VER);

        return mv;
    }

    @PostMapping("systems")
    public void postSystems(HttpServletRequest request, HttpServletResponse response) {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        String dHomeURL = defaultIfBlank(data, ConfigurationItem.HomeURL);
        if (!RegexUtils.isUrl(dHomeURL)) {
            writeFailure(response, getLang(request, "SomeInvalid", "HomeUrl"));
            return;
        }

        // 验证数字参数
        ConfigurationItem[] validNumbers = new ConfigurationItem[]{
                ConfigurationItem.RecycleBinKeepingDays,
                ConfigurationItem.RevisionHistoryKeepingDays,
                ConfigurationItem.DBBackupsKeepingDays
        };
        for (ConfigurationItem item : validNumbers) {
            String number = defaultIfBlank(data, item);
            if (!NumberUtils.isNumber(number)) {
                data.put(item.name(), item.getDefaultValue());
            }
        }

        setValues(data);
        Application.getBean(RebuildWebConfigurer.class).init();

        writeSuccess(response);
    }

    @GetMapping("integration/storage")
    public ModelAndView pageIntegrationStorage() {
        ModelAndView mv = createModelAndView("/admin/integration/storage-qiniu");
        mv.getModel().put("storageAccount",
                starsAccount(RebuildConfiguration.getStorageAccount(), 0, 1));
        mv.getModel().put("storageStatus", QiniuCloud.instance().available());

        // 存储大小
        long size = QiniuCloud.storageSize();
        mv.getModel().put("_StorageSize", FileUtils.byteCountToDisplaySize(size));
        // 云存储
        mv.getModel().put("_StorageCloud", QiniuCloud.instance().available());

        return mv;
    }

    @PostMapping("integration/storage")
    public void postIntegrationStorage(HttpServletRequest request, HttpServletResponse response) {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        String dStorageURL = defaultIfBlank(data, ConfigurationItem.StorageURL);
        String dStorageBucket = defaultIfBlank(data, ConfigurationItem.StorageBucket);
        String dStorageApiKey = defaultIfBlank(data, ConfigurationItem.StorageApiKey);
        String dStorageApiSecret = defaultIfBlank(data, ConfigurationItem.StorageApiSecret);

        if (dStorageURL.startsWith("//")) {
            dStorageURL = "https:" + dStorageURL;
        }
        if (!RegexUtils.isUrl(dStorageURL)) {
            writeFailure(response, getLang(request, "SomeInvalid", "StorageDomain"));
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
            writeFailure(response, getLang(request, "ConfInvalid") + " : " + ex.response.error);
        } catch (Exception ex) {
            writeFailure(response, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
        }
    }

    @GetMapping("integration/submail")
    public ModelAndView pageIntegrationSubmail() {
        ModelAndView mv = createModelAndView("/admin/integration/submail");
        mv.getModel().put("smsAccount",
                starsAccount(RebuildConfiguration.getSmsAccount(), 1));
        mv.getModel().put("mailAccount",
                starsAccount(RebuildConfiguration.getMailAccount(), 1));
        return mv;
    }

    @PostMapping("integration/submail")
    public void postIntegrationSubmail(HttpServletRequest request, HttpServletResponse response) {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        String dMailAddr = defaultIfBlank(data, ConfigurationItem.MailAddr);
        if (!RegexUtils.isEMail(dMailAddr)) {
            writeFailure(response, getLang(request, "SomeInvalid", "MailServAddr"));
            return;
        }

        setValues(data);
        writeSuccess(response);
    }

    @PostMapping("integration/submail/test")
    public void testSubmail(HttpServletRequest request, HttpServletResponse response) {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);
        String type = getParameterNotNull(request, "type");
        String receiver = getParameterNotNull(request, "receiver");

        String sent = null;
        if ("SMS".equalsIgnoreCase(type)) {
            if (!RegexUtils.isCNMobile(receiver)) {
                writeFailure(response, getLang(request, "SomeInvalid", "Mobile"));
                return;
            }

            String[] specAccount = new String[]{
                    data.getString("SmsUser"), data.getString("SmsPassword"),
                    data.getString("SmsSign")
            };
            if (specAccount[1].contains("**********")) {
                specAccount[1] = RebuildConfiguration.get(ConfigurationItem.SmsPassword);
            }

            String content = getLang(request, "SendTestMessage", "Sms");
            sent = SMSender.sendSMS(receiver, content, specAccount);

        } else if ("EMAIL".equalsIgnoreCase(type)) {
            if (!RegexUtils.isEMail(receiver)) {
                writeFailure(response, getLang(request, "SomeInvalid", "Email"));
                return;
            }

            String[] specAccount = new String[]{
                    data.getString("MailUser"), data.getString("MailPassword"),
                    data.getString("MailAddr"), data.getString("MailName")
            };
            if (specAccount[1].contains("**********")) {
                specAccount[1] = RebuildConfiguration.get(ConfigurationItem.MailPassword);
            }

            String content = getLang(request, "SendTestMessage", "Email");
            sent = SMSender.sendMail(receiver, content, content, true, specAccount);
        }

        if (sent != null) {
            writeSuccess(response, sent);
        } else {
            writeFailure(response, getLang(request, "SendTestError"));
        }
    }

    @RequestMapping(value = "integration/submail/stats")
    public void statsSubmail(HttpServletResponse response) {
        final Date xday = CalendarUtils.clearTime(CalendarUtils.addDay(-90));
        final String sql = "select date_format(sendTime,'%Y-%m-%d'),count(sendId) from SmsendLog" +
                " where type = ? and sendTime > ? group by date_format(sendTime,'%Y-%m-%d')";

        Object[][] sms = Application.createQueryNoFilter(sql)
                .setParameter(1, 1)
                .setParameter(2, xday)
                .array();
        Arrays.sort(sms, Comparator.comparing(o -> o[0].toString()));

        Object[] smsCount = Application.createQueryNoFilter(
                "select count(sendId) from SmsendLog where type = ?")
                .setParameter(1, 1)
                .unique();

        Object[][] email = Application.createQueryNoFilter(sql)
                .setParameter(1, 2)
                .setParameter(2, xday)
                .array();
        Arrays.sort(email, Comparator.comparing(o -> o[0].toString()));

        Object[] emailCount = Application.createQueryNoFilter(
                "select count(sendId) from SmsendLog where type = ?")
                .setParameter(1, 2)
                .unique();

        JSONObject data = JSONUtils.toJSONObject(
                new String[]{"sms", "email", "smsCount", "emailCount"},
                new Object[]{sms, email, smsCount, emailCount});
        writeSuccess(response, data);
    }

    private String[] starsAccount(String[] account, int... index) {
        if (account == null) {
            return null;
        }
        for (int i : index) {
            account[i] = CommonsUtils.stars(account[i]);
        }
        return account;
    }

    private String defaultIfBlank(JSONObject data, ConfigurationItem item) {
        return StringUtils.defaultIfBlank(data.getString(item.name()), RebuildConfiguration.get(item));
    }

    private void setValues(JSONObject data) {
        for (Map.Entry<String, Object> e : data.entrySet()) {
            try {
                ConfigurationItem item = ConfigurationItem.valueOf(e.getKey());
                RebuildConfiguration.set(item, e.getValue());
            } catch (Exception ex) {
                LOG.error("Invalid item : " + e.getKey() + " = " + e.getValue());
            }
        }
    }
}
