/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.util.Auth;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.DataDesensitized;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import com.rebuild.web.RebuildWebConfigurer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

/**
 * 系统配置
 *
 * @author Zixin (RB)
 * @see RebuildConfiguration
 * @see ConfigurationItem
 * @since 09/20/2018
 */
@Slf4j
@RestController
@RequestMapping("/admin/")
public class ConfigurationController extends BaseController {

    public static final String ETAG_DIMGLOGOTIME = "dimgLogoTime";
    public static final String ETAG_DIMGBGIMGTIME = "dimgBgimgTime";

    @GetMapping("systems")
    public ModelAndView pageSystems() {
        ModelAndView mv = createModelAndView("/admin/system-cfg");
        for (ConfigurationItem item : ConfigurationItem.values()) {
            mv.getModel().put(item.name(), RebuildConfiguration.get(item));
        }

        // Available langs
        mv.getModel().put("availableLangs", JSON.toJSON(Application.getLanguage().availableLocales()));

        JSONObject auth = License.queryAuthority();
        mv.getModel().put("LicenseType",
                auth.getString("authType") + " (" + auth.getString("authObject") + ")");
        mv.getModel().put("Version", Application.VER);

        return mv;
    }

    @PostMapping("systems")
    public RespBody postSystems(@RequestBody JSONObject data) {
        String dHomeURL = defaultIfBlank(data, ConfigurationItem.HomeURL);
        if (!RegexUtils.isUrl(dHomeURL)) {
            return RespBody.errorl("无效主页地址/域名");
        }

        // 验证数字参数
        ConfigurationItem[] validNumbers = new ConfigurationItem[] {
                ConfigurationItem.RecycleBinKeepingDays,
                ConfigurationItem.RevisionHistoryKeepingDays,
                ConfigurationItem.DBBackupsKeepingDays,
                ConfigurationItem.PasswordExpiredDays
        };
        for (ConfigurationItem item : validNumbers) {
            String number = defaultIfBlank(data, item);
            if (!NumberUtils.isNumber(number)) {
                data.put(item.name(), item.getDefaultValue());
            }
        }

        String dLOGO = data.getString(ConfigurationItem.LOGO.name());
        String dLOGOWhite = data.getString(ConfigurationItem.LOGOWhite.name());
        if (dLOGO != null || dLOGOWhite != null) {
            Application.getCommonsCache().evict(ETAG_DIMGLOGOTIME);
        }
        String dCustomWallpaper = data.getString(ConfigurationItem.CustomWallpaper.name());
        if (dCustomWallpaper != null) {
            Application.getCommonsCache().evict(ETAG_DIMGBGIMGTIME);
        }

        setValues(data);
        Application.getBean(RebuildWebConfigurer.class).init();

        return RespBody.ok();
    }

    @GetMapping("integration/storage")
    public ModelAndView pageIntegrationStorage() {
        ModelAndView mv = createModelAndView("/admin/integration/storage-qiniu");
        mv.getModel().put("storageAccount",
                starsAccount(RebuildConfiguration.getStorageAccount(), 0, 1));
        mv.getModel().put("storageStatus", QiniuCloud.instance().available());

        // 存储大小
        long size = QiniuCloud.getStorageSize();
        mv.getModel().put("_StorageSize", FileUtils.byteCountToDisplaySize(size));
        // 云存储
        mv.getModel().put("_StorageCloud", QiniuCloud.instance().available());

        return mv;
    }

    @PostMapping("integration/storage")
    public RespBody postIntegrationStorage(@RequestBody JSONObject data) {
        String dStorageUrl = defaultIfBlank(data, ConfigurationItem.StorageURL);
        String dStorageBucket = defaultIfBlank(data, ConfigurationItem.StorageBucket);
        String dStorageApiKey = defaultIfBlank(data, ConfigurationItem.StorageApiKey);
        String dStorageApiSecret = defaultIfBlank(data, ConfigurationItem.StorageApiSecret);

        if (!dStorageUrl.endsWith("/")) {
            dStorageUrl = dStorageUrl + "/";
            data.put(ConfigurationItem.StorageURL.name(), dStorageUrl);  // fix
        }

        if (dStorageUrl.startsWith("http://") || dStorageUrl.startsWith("https://")) {
            // OK
        } else {
            if (dStorageUrl.startsWith("//")) {
                dStorageUrl = "https:" + dStorageUrl;
            } else {
                dStorageUrl = "http://" + dStorageUrl;
                data.put(ConfigurationItem.StorageURL.name(), dStorageUrl);  // fix
            }
        }

        if (!RegexUtils.isUrl(dStorageUrl)) {
            return RespBody.errorl("无效访问域名");
        }

        try {
            // Test
            Auth auth = Auth.create(dStorageApiKey, dStorageApiSecret);
            BucketManager bucketManager = new BucketManager(auth, QiniuCloud.CONFIGURATION);
            bucketManager.getBucketInfo(dStorageBucket);

            setValues(data);

            QiniuCloud.instance().initAuth();
            Application.getBean(RebuildWebConfigurer.class).init();

            return RespBody.ok();

        } catch (QiniuException ex) {
            return RespBody.error(Language.L("无效配置参数 : %s", ex.response.error));
        } catch (Exception ex) {
            return RespBody.error(ThrowableUtils.getRootCause(ex).getLocalizedMessage());
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
    public RespBody postIntegrationSubmail(@RequestBody JSONObject data) {
        String dMailAddr = defaultIfBlank(data, ConfigurationItem.MailAddr);
        if (!RegexUtils.isEMail(dMailAddr)) {
            return RespBody.errorl("无效发件人地址");
        }

        setValues(data);
        return RespBody.ok();
    }

    @PostMapping("integration/submail/test")
    public RespBody testSubmail(@RequestBody JSONObject data, HttpServletRequest request) {
        String type = getParameterNotNull(request, "type");
        String receiver = getParameterNotNull(request, "receiver");

        String sent = null;
        if ("SMS".equalsIgnoreCase(type)) {
            if (!RegexUtils.isCNMobile(receiver)) {
                return RespBody.errorl("无效手机号码");
            }

            String[] specAccount = new String[]{
                    data.getString("SmsUser"), data.getString("SmsPassword"),
                    data.getString("SmsSign")
            };
            if (specAccount[1].contains("*")) {
                specAccount[1] = RebuildConfiguration.get(ConfigurationItem.SmsPassword);
            }

            String content = Language.L("收到此消息说明你的短信服务配置正确");
            sent = SMSender.sendSMS(receiver, content, specAccount);

        } else if ("EMAIL".equalsIgnoreCase(type)) {
            if (!RegexUtils.isEMail(receiver)) {
                return RespBody.errorl("无效邮箱地址");
            }

            String[] specAccount = new String[]{
                    data.getString("MailUser"), data.getString("MailPassword"),
                    data.getString("MailAddr"), data.getString("MailName"),
                    data.getString("MailCc"),
                    data.getString("MailSmtpServer")
            };
            if (specAccount[1].contains("*")) {
                specAccount[1] = RebuildConfiguration.get(ConfigurationItem.MailPassword);
            }

            String content = Language.L("收到此消息说明你的邮件服务配置正确");
            sent = SMSender.sendMail(receiver, content, content, true, specAccount);
        }

        if (sent != null) {
            return RespBody.ok(sent);
        } else {
            return RespBody.errorl("测试发送失败，请检查你的配置");
        }
    }

    @GetMapping(value = "integration/submail/stats")
    public JSON statsSubmail() {
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

        return JSONUtils.toJSONObject(
                new String[] { "sms", "email", "smsCount", "emailCount" },
                new Object[] { sms, email, smsCount, emailCount });
    }

    private String[] starsAccount(String[] account, int... index) {
        if (account == null || account.length == 0) return null;

        for (int i : index) {
            account[i] = DataDesensitized.any(account[i]);
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
                log.error("Invalid item : {} = {}", e.getKey(), e.getValue(), ex);
            }
        }
    }

    // DingTalk

    @GetMapping("integration/dingtalk")
    public ModelAndView pageIntegrationDingtalk() {
        RbAssert.isCommercial(
                Language.L("免费版不支持钉钉集成 [(查看详情)](https://getrebuild.com/docs/rbv-features)"));

        ModelAndView mv = createModelAndView("/admin/integration/dingtalk");
        for (ConfigurationItem item : ConfigurationItem.values()) {
            String name = item.name();
            if (name.startsWith("Dingtalk")) {
                String value = RebuildConfiguration.get(item);

                if (value != null && item == ConfigurationItem.DingtalkAppsecret) {
                    value = DataDesensitized.any(value);
                }
                mv.getModel().put(name, value);

                if (ID.isId(value) && item == ConfigurationItem.DingtalkSyncUsersRole) {
                    mv.getModel().put(name + "Label", UserHelper.getName(ID.valueOf(value)));
                }
            }
        }

        String homeUrl = RebuildConfiguration.getHomeUrl("/user/dingtalk");
        mv.getModel().put("_DingtalkHomeUrl", homeUrl);
        String[] authCallUrl = homeUrl.split("/");
        mv.getModel().put("_DingtalkAuthCallUrl", authCallUrl[0] + "//" + authCallUrl[2] + "/");

        return mv;
    }

    @PostMapping("integration/dingtalk")
    public RespBody postIntegrationDingtalk(@RequestBody JSONObject data) {
        setValues(data);
        return RespBody.ok();
    }

    // WxWork

    @GetMapping("integration/wxwork")
    public ModelAndView pageIntegrationWxwork() {
        RbAssert.isCommercial(
                Language.L("免费版不支持企业微信集成 [(查看详情)](https://getrebuild.com/docs/rbv-features)"));

        ModelAndView mv = createModelAndView("/admin/integration/wxwork");
        for (ConfigurationItem item : ConfigurationItem.values()) {
            String name = item.name();
            if (name.startsWith("Wxwork")) {
                String value = RebuildConfiguration.get(item);

                if (value != null && item == ConfigurationItem.WxworkSecret) {
                    value = DataDesensitized.any(value);
                }
                mv.getModel().put(name, value);

                if (ID.isId(value) && item == ConfigurationItem.WxworkSyncUsersRole) {
                    mv.getModel().put(name + "Label", UserHelper.getName(ID.valueOf(value)));
                }
            }
        }

        String homeUrl = RebuildConfiguration.getHomeUrl("/user/wxwork");
        mv.getModel().put("_WxworkHomeUrl", homeUrl);
        mv.getModel().put("_WxworkAuthCallUrl", homeUrl.split("//")[1].split("/")[0]);

        return mv;
    }

    @PostMapping("integration/wxwork")
    public RespBody postIntegrationWxwork(@RequestBody JSONObject data) {
        setValues(data);
        return RespBody.ok();
    }

    // SAML

    @GetMapping("integration/sso-saml")
    public ModelAndView pageIntegrationSsoSaml() {
        RbAssert.isCommercial(
                Language.L("免费版不支持企业身份认证 [(查看详情)](https://getrebuild.com/docs/rbv-features)"));

        ModelAndView mv = createModelAndView("/admin/integration/sso-saml");
        for (ConfigurationItem item : ConfigurationItem.values()) {
            String name = item.name();
            if (name.startsWith("Saml")) {
                String value = RebuildConfiguration.get(item);
//                if (value != null && item == ConfigurationItem.SamlIdPCert) {
//                    String[] vs = value.split("\n");
//                    value = StringUtils.join(
//                            new String[] { vs[0], vs[1], "**********", vs[vs.length - 2], vs[vs.length - 1] }, "\n");
//                }
                mv.getModel().put(name, value);
            }
        }

        mv.getModel().put("_SamlSpEndpoint", RebuildConfiguration.getHomeUrl("/user/sso-saml2-login"));
        mv.getModel().put("_SamlSpSloEndpoint", RebuildConfiguration.getHomeUrl("/user/logout"));
        mv.getModel().put("_SamlSpEntityid", RebuildConfiguration.getHomeUrl());

        return mv;
    }

    @PostMapping("integration/sso-saml")
    public RespBody postIntegrationSsoSaml(@RequestBody JSONObject data) {
        setValues(data);
        return RespBody.ok();
    }
}
