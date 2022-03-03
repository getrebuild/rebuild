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
        if (useCache && Application.isReady()) {
            result = (JSONObject) Application.getCommonsCache().getx("IPLocation2" + ip);
            if (result != null) {
                return result;
            }
        }

        result = new JSONObject();
        result.put("ip", ip);

        JSONObject fetchTry;

//        // #1
//        fetchTry = getJSON(String.format("https://ip.taobao.com/outGetIpInfo?ip=%s&accessKey=alibaba-inc", ip));
//        if (fetchTry != null && fetchTry.getIntValue("code") == 0) {
//            fetchTry = fetchTry.getJSONObject("data");
//            String c = fetchTry.getString("country");
//            if ("local".equalsIgnoreCase(fetchTry.getString("isp_id")) || "xx".equalsIgnoreCase(c)) {
//                result.put("country", "R");
//            } else {
//                result.put("country", "xx".equalsIgnoreCase(c) ? "" : c);
//                c = fetchTry.getString("region");
//                result.put("region", "xx".equalsIgnoreCase(c) ? "" : c);
//                c = fetchTry.getString("city");
//                result.put("city", "xx".equalsIgnoreCase(c) ? "" : c);
//            }
//            return result;
//        }

        // #2
        fetchTry = getJSON(String.format("https://ipapi.co/%s/json/", ip));
        if (fetchTry != null) {
            if (fetchTry.getString("country") != null) {
                result.put("country", fetchTry.getString("country"));
                result.put("region", fetchTry.getString("region"));
                result.put("city", fetchTry.getString("city"));
            } else if (fetchTry.getBooleanValue("reserved")) {
                result.put("country", "R");
            }
            return result;
        }

        // #3
        fetchTry = getJSON(String.format("http://ip-api.com/json/%s", ip));
        if (fetchTry != null) {
            String message = fetchTry.getString("message");
            if (fetchTry.getString("countryCode") != null) {
                result.put("country", fetchTry.getString("countryCode"));
                result.put("region", fetchTry.getString("regionName"));
                result.put("city", fetchTry.getString("city"));
                return result;
            } else if (message != null && (message.contains("private") || message.contains("reserved"))) {
                result.put("country", "R");
                return result;
            }
        }

        if (result.getString("country") == null) {
            result.put("country", "N");
        }

        if (Application.isReady()) {
            Application.getCommonsCache().putx("IPLocation2" + ip, result, CommonsCache.TS_DAY * 90);
        }
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
