/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.helper.cache.CommonCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 位置工具
 *
 * @author devezhao
 * @since 2019/8/28
 */
public class LocationUtils {

    private static final Log LOG = LogFactory.getLog(LocationUtils.class);

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
        JSONObject result = null;
        if (useCache) {
            result = (JSONObject) Application.getCommonCache().getx("IPLocation2" + ip);
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
            LOG.warn("Use backup IP location service : " + loc);

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

        Application.getCommonCache().putx("IPLocation2" + ip, result, CommonCache.TS_DAY * 7);
        return result;
    }

    /**
     * @param url
     * @return
     */
    private static JSONObject getJSON(String url) {
        try {
            return JSON.parseObject(CommonsUtils.get(url));
        } catch (Exception e) {
            LOG.debug("Error occured : " + url + " >> " + e);
        }
        return null;
    }
}
