/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.sdk;

import cn.devezhao.commons.EncryptUtils;
import com.alibaba.fastjson.JSON;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * OpenAPI SDK for Rebuild. 建议单例使用
 *
 * @author ZHAO
 * @since 2019-07-23
 */
public class OpenApiSDK {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private static final JSON ERROR_REQ = JSON.parseObject("{ error_code:600, error_msg:'Http request failed' }");

    private static final Log LOG = LogFactory.getLog(OpenApiSDK.class);

    final private String appId;
    final private String appSecret;
    final private String baseUrl;

    final private OkHttpClient okHttpClient;

    /**
     * @param appId
     * @param appSecret
     */
    public OpenApiSDK(String appId, String appSecret) {
        this(appId, appSecret, "https://nightly.getrebuild.com/gw/api/");
    }

    /**
     * @param appId
     * @param appSecret
     * @param baseUrl
     */
    public OpenApiSDK(String appId, String appSecret, String baseUrl) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.baseUrl = baseUrl;

        this.okHttpClient = new OkHttpClient().newBuilder()
                .retryOnConnectionFailure(false)
                .callTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    /**
     * @param params 业务参数
     * @return
     */
    public String sign(Map<String, Object> params) {
        return sign(params, "MD5");
    }

    /**
     * @param reqParams
     * @param signType MD5|SHA1
     * @return
     */
    public String sign(Map<String, Object> reqParams, String signType) {
        Map<String, Object> sortMap = new TreeMap<>();
        if (reqParams != null && !reqParams.isEmpty()) {
            sortMap.putAll(reqParams);
        }
        sortMap.put("appid", this.appId);
        sortMap.put("timestamp", System.currentTimeMillis() / 1000);  // in sec
        sortMap.put("sign_type", signType);

        StringBuilder sign = new StringBuilder();
        for (Map.Entry<String, Object> e : sortMap.entrySet()) {
            sign.append(e.getKey())
                    .append('=')
                    .append(e.getValue())
                    .append('&');
        }

        final String signUrl = sign.toString() + "sign=";

        // 拼接
        sign.append(this.appId)
                .append('.')
                .append(this.appSecret);

        if ("MD5".equals(signType)) {
            return signUrl + EncryptUtils.toMD5Hex(sign.toString());
        } else if ("SHA1".equals(signType)) {
            return signUrl + EncryptUtils.toSHA1Hex(sign.toString());
        } else {
            throw new IllegalArgumentException("signType=" + signType);
        }
    }

    /**
     * @param url
     * @return
     * @throws IOException
     */
    public JSON httpGet(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String resp = Objects.requireNonNull(response.body()).string();
            return (JSON) JSON.parse(resp);
        }
    }

    /**
     * @param url
     * @param post
     * @return
     * @throws IOException
     */
    public JSON httpPost(String url, JSON post) throws IOException {
        RequestBody body = RequestBody.create(post.toJSONString(), JSON_TYPE);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String resp = Objects.requireNonNull(response.body()).string();
            return (JSON) JSON.parse(resp);
        }
    }

    /**
     * @param apiName
     * @param reqParams
     * @return
     */
    public JSON get(String apiName, Map<String, Object> reqParams) {
        final String apiUrl = buildApiUrl(apiName, reqParams);
        try {
            return httpGet(apiUrl);
        } catch (Exception e) {
            LOG.error("Api (GET) failed : " + apiUrl, e);
            return ERROR_REQ;
        }
    }

    /**
     * @param apiName
     * @param reqParams
     * @param post
     * @return
     */
    public JSON post(String apiName, Map<String, Object> reqParams, JSON post) {
        final String apiUrl = buildApiUrl(apiName, reqParams);
        try {
            return httpPost(apiUrl, post);
        } catch (Exception e) {
            LOG.error("Api (POST) failed : " + apiUrl, e);
            return ERROR_REQ;
        }
    }

    private String buildApiUrl(String apiName, Map<String, Object> reqParams) {
        StringBuilder apiUrl = new StringBuilder(baseUrl);
        if (!baseUrl.endsWith("/")) apiUrl.append('/');

        if (apiName.startsWith("/")) apiName = apiName.substring(1);
        apiUrl.append(apiName);

        apiUrl.append('?').append(sign(reqParams));
        return apiUrl.toString();
    }
}
