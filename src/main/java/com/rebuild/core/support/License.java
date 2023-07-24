/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ExpiresMap;
import cn.devezhao.commons.identifier.ComputerIdentifier;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * !!!! 请勿修改或删除本文件
 * !!!! 请严格遵守《REBUILD 用户服务协议》https://getrebuild.com/legal/service-terms
 */
@Slf4j
public final class License {

    private static final String OSA_KEY = "IjkMHgq94T7s7WkP";
    private static final String TEMP_SN = "SN000-00000000-000000000";

    private static String USE_SN;
    private static Boolean USE_RBV;

    public static String SN() {
        if (USE_SN != null) return USE_SN;

        String SN = RebuildConfiguration.get(ConfigurationItem.SN, true);
        if (SN != null) {
            if (Application.isReady()) USE_SN = SN;
            return SN;
        }

        if (!Application.isReady()) return TEMP_SN;

        JSONObject newsn = siteApi("api/authority/new?ver=" + Application.VER);
        SN = newsn.getString("sn");
        if (SN != null) {
            RebuildConfiguration.setValue(ConfigurationItem.SN.name(), SN);
            siteApiNoCache("api/authority/query");
        }

        if (SN == null) {
            SN = String.format("RB%s%s-%s-%s",
                    Application.VER.charAt(0),
                    Locale.getDefault().getCountry().substring(0, 2),
                    ComputerIdentifier.generateIdentifierKey(),
                    CodecUtils.randomCode(9)).toUpperCase();
            RebuildConfiguration.setValue(ConfigurationItem.SN.name(), SN);
            siteApiNoCache("api/authority/query");
        }

        USE_SN = SN;
        return SN;
    }

    public static JSONObject queryAuthority() {
        JSONObject auth = siteApi("api/authority/query");
        String error;
        if ((error = auth.getString("error")) != null) {
            auth = JSONUtils.toJSONObject(
                    new String[] { "sn", "authType", "authObject", "authExpires" },
                    new String[] { SN(), "开源社区版", "GitHub", "无" });
        }
        if ("BLOCKED".equals(error)) System.exit(110);
        return auth;
    }

    public static int getCommercialType() {
        JSONObject auth = queryAuthority();
        Integer authType = auth.getInteger("authTypeInt");
        return authType == null ? 0 : authType;
    }

    public static boolean isCommercial() {
        return getCommercialType() > 0;
    }

    public static boolean isRbvAttached() {
        if (USE_RBV != null) return USE_RBV;
        if (!isCommercial()) {
            USE_RBV = false;
            return false;
        }

        try {
            Application.getContext().getBean("@rbv");
            USE_RBV = true;
        } catch (BeansException norbv) {
            USE_RBV = false;
        }
        return USE_RBV;
    }

    public static JSONObject siteApi(String api) {
        return siteApi(api, ExpiresMap.HOUR_IN_SECOND * 2, null);
    }

    public static JSONObject siteApiNoCache(String api) {
        return siteApi(api, 0, null);
    }

    private static final ExpiresMap<String, JSONObject> MCACHED = new ExpiresMap<>();
    /**
     * @param api
     * @param t
     * @param domain
     * @return
     */
    private static JSONObject siteApi(String api, int t, String domain) {
        if (t > 0) {
            JSONObject c = MCACHED.get(api, t);
            if (c != null) return c.clone();
        } else {
            MCACHED.remove(api);
        }

        Map<String, String> hs = new HashMap<>();
        hs.put("X-SiteApi-K", OSA_KEY);
        if (!api.contains("/authority/new")) hs.put("X-SiteApi-SN", SN());

        try {
            String apiUrl = StringUtils.defaultIfEmpty(domain, "https://getrebuild.com/") + api;
            String result = OkHttpUtils.get(apiUrl, hs);

            if (JSONUtils.wellFormat(result)) {
                JSONObject o = JSON.parseObject(result);

                String hasError = o.getString("error");
                if (hasError != null) {
                    log.error("Result return error : {}", result);
                } else {
                    MCACHED.put(api, o);
                }
                return o.clone();

            } else {
                log.error("Bad result format : {}", result);
            }

        } catch (Exception ex) {
            log.error("Call site api `{}` error : {}", api.split("\\?")[0], ex.toString());
        }

        if (domain == null) {
            return siteApi(api, t, "http://rebuild.ruifang-tech.com/");
        } else {
            return JSONUtils.toJSONObject("error", "Call site api failed");
        }
    }
}
