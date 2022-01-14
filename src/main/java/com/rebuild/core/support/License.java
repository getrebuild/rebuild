/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.CodecUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.utils.HttpUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;

import java.util.Locale;

/**
 * @author ZHAO
 * @since 2019-08-23
 */
@Slf4j
public final class License {

    private static final String OSA_KEY = "IjkMHgq94T7s7WkP";
    private static final String TEMP_SN = "SN-000000-00000000000000";

    private static String USE_SN;
    private static Boolean USE_RBV;

    public static String SN() {
        if (USE_SN != null) return USE_SN;

        String SN = RebuildConfiguration.get(ConfigurationItem.SN, true);
        if (SN != null) {
            USE_SN = SN;
            return SN;
        }

        if (!Application.isReady()) return TEMP_SN;

        JSONObject newsn = siteApi("api/authority/new?ver=" + Application.VER);
        SN = newsn == null ? null : newsn.getString("sn");
        if (SN != null) {
            RebuildConfiguration.set(ConfigurationItem.SN, SN);
        }

        if (SN == null) {
            SN = String.format("ZR%d%s-%s",
                    Application.BUILD,
                    Locale.getDefault().getCountry().substring(0, 2),
                    CodecUtils.randomCode(14)).toUpperCase();
            RebuildConfiguration.set(ConfigurationItem.SN, SN);
        }

        USE_SN = SN;
        return SN;
    }

    public static JSONObject queryAuthority(boolean useCache) {
        JSONObject auth = siteApi("api/authority/query", useCache);
        if (auth == null || auth.getString("error") != null) {
            auth = JSONUtils.toJSONObject(
                    new String[] { "sn", "authType", "authObject", "authExpires" },
                    new String[] { SN(), "开源社区版", "GitHub", "无" });
        }
        return auth;
    }

    public static int getCommercialType() {
        JSONObject auth = queryAuthority(true);
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
        return siteApi(api, false);
    }

    public static JSONObject siteApi(String api, boolean useCache) {
        if (useCache) {
            Object o = Application.getCommonsCache().getx(api);
            if (o != null) {
                return (JSONObject) o;
            }
        }

        String apiUrl = "https://getrebuild.com/" + api + (api.contains("?") ? "&" : "?") + "k=" + OSA_KEY;
        if (!api.contains("/authority/new")) apiUrl += "&sn=" + SN();

        try {
            String result = HttpUtils.get(apiUrl);
            if (JSONUtils.wellFormat(result)) {
                JSONObject o = JSON.parseObject(result);

                String hasError = o.getString("error");
                if (hasError != null) {
                    log.error("Bad result : {}", result);
                } else {
                    Application.getCommonsCache().putx(api, o, CommonsCache.TS_HOUR);
                }
                return o;
            } else {
                log.error("Bad result format : {}", result);
            }

        } catch (Exception ex) {
            log.error("Call site api `{}` error : {}", api, ex.toString());
        }
        return null;
    }
}
