/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.api.sdk;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.EncryptUtils;
import com.alibaba.fastjson.JSON;
import org.apache.tools.ant.taskdefs.Get;

import java.util.Map;
import java.util.TreeMap;

/**
 * OpenAPI SDK for Rebuild. 建议单例使用
 *
 * @author ZHAO
 * @since 2019-07-23
 */
public class OpenApiSDK {

    private String appId;
    private String appSecret;

    /**
     * @param appId
     * @param appSecret
     */
    public OpenApiSDK(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    /**
     * @param params 业务参数
     * @return
     */
    public String signMD5(Map<String, Object> params) {
        return sign(params, "MD5");
    }

    /**
     * @param params 业务参数
     * @return
     */
    public String signSHA1(Map<String, Object> params) {
        return sign(params, "SHA1");
    }

    /**
     * @param params
     * @param signType MD5|SHA1
     * @return
     */
    protected String sign(Map<String, Object> params, String signType) {
        Map<String, Object> sortMap = new TreeMap<>();
        if (params != null && !params.isEmpty()) {
            sortMap.putAll(params);
        }
        sortMap.put("appid", this.appId);
        sortMap.put("timestamp", System.currentTimeMillis() / 1000);  // in sec
        sortMap.put("sign_type", signType);

        StringBuffer sign = new StringBuffer();
        for (Map.Entry<String, Object> e : sortMap.entrySet()) {
            sign.append(e.getKey())
                    .append('=')
                    .append(e.getValue())
                    .append('&');
        }

        String signUrl = sign.toString() + "sign=";
        sign.append(this.appId)
                .append('.')
                .append(this.appSecret);
        if ("MD5".equals(signType)) {
            signUrl += EncryptUtils.toMD5Hex(sign.toString());
        } else if ("SHA1".equals(signType)) {
            signUrl += EncryptUtils.toSHA1Hex(sign.toString());
        } else {
            throw new IllegalArgumentException("signType=" + signType);
        }
        return signUrl;
    }

    /**
     * @param url
     * @return
     */
    public JSON get(String url) {
        // TODO
        return null;
    }

    /**
     * @param url
     * @param post
     * @return
     */
    public JSON post(String url, JSON post) {
        // TODO
        return null;
    }
}
