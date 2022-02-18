/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CommonsCache;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 位置工具
 *
 * @author devezhao
 * @since 2019/8/28
 */
@Slf4j
public class LocationUtils {

    private static final Pattern PRIVATE_IP = Pattern.compile("(localhost)|" +
            "(^127\\.)|(^10\\.)|(^172\\.1[6-9]\\.)|(^172\\.2[0-9]\\.)|(^172\\.3[0-1]\\.)|(^192\\.168\\.)");

    /**
     * 获取 IP 所在位置
     *
     * @param ip ip v4 or v6
     * @return
     */
    public static JSON getLocation(String ip) {
        return getLocation(ip, true);
    }

    /**
     * 获取 IP 所在位置
     *
     * @param ip
     * @param useCache 7天缓存
     * @return
     */
    public static JSON getLocation(String ip, boolean useCache) {
        if (PRIVATE_IP.matcher(ip).find()) {
            return JSONUtils.toJSONObject(new String[] { "ip", "country"}, new String[] { ip, "R" });
        }

        JSONObject result;
        if (useCache) {
            result = (JSONObject) Application.getCommonsCache().getx("IPLocation2" + ip);
            if (result != null) {
                return result;
            }
        }

        result = new JSONObject();
        result.put("ip", ip);

        JSONObject loc = getJSON(String.format("https://ipapi.co/%s/json/", ip));
        if (loc != null) {
            if (loc.getString("country") != null) {
                result.put("country", loc.getString("country"));
                result.put("region", loc.getString("region"));
                result.put("city", loc.getString("city"));
            } else if (loc.getBooleanValue("reserved")) {
                result.put("country", "R");
            }
        }

        if (result.getString("country") == null) {
            loc = getJSON(String.format("http://ip-api.com/json/%s", ip));
            log.warn("Use backup IP location service : " + loc);

            if (loc != null) {
                String message = loc.getString("message");
                if (loc.getString("countryCode") != null) {
                    result.put("country", loc.getString("countryCode"));
                    result.put("region", loc.getString("regionName"));
                    result.put("city", loc.getString("city"));
                } else if (message != null && (message.contains("private") || message.contains("reserved"))) {
                    result.put("country", "R");
                }
            }
        }

        if (result.getString("country") == null) {
            result.put("country", "N");
        }

        Application.getCommonsCache().putx("IPLocation2" + ip, result, CommonsCache.TS_DAY * 90);
        return result;
    }

    /**
     * @param url
     * @return
     */
    private static JSONObject getJSON(String url) {
        try {
            return JSON.parseObject(HttpUtils.get(url));
        } catch (Exception e) {
            log.debug("Error occured : " + url + " >> " + e);
        }
        return null;
    }
}
