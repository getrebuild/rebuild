/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.sdk;

import cn.devezhao.commons.EncryptUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
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
                .connectTimeout(30, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 签名（MD5）
     *
     * @param reqParams
     * @return
     * @see #sign(Map, String)
     */
    public String sign(Map<String, Object> reqParams) {
        return sign(reqParams, "MD5");
    }

    /**
     * 签名
     *
     * @param reqParams
     * @param signType
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

        final String signUrl = sign + "sign=";

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
     * GET 请求
     *
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
     * POST 请求
     *
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

    /**
     * @param apiName
     * @param reqParams
     * @return
     */
    private String buildApiUrl(String apiName, Map<String, Object> reqParams) {
        StringBuilder apiUrl = new StringBuilder(baseUrl);
        if (!baseUrl.endsWith("/")) apiUrl.append('/');

        if (apiName.startsWith("/")) apiName = apiName.substring(1);
        apiUrl.append(apiName);

        apiUrl.append('?').append(sign(reqParams));
        return apiUrl.toString();
    }

    /**
     * 文件下载
     *
     * @param filePath 文件（相对）路径
     * @param dest 存储到指定文件
     * @throws IOException
     */
    public boolean fileDownload(String filePath, File dest) throws IOException {
        JSONObject res = (JSONObject) get("file/download", Collections.singletonMap("file", filePath));
        JSONObject data = Objects.requireNonNull(res.getJSONObject("data"), "Bad result : " + res);

        String downloadUrl = data.getString("download_url");

        Request request = new Request.Builder().url(downloadUrl).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            try (InputStream is = Objects.requireNonNull(response.body()).byteStream()) {
                try (BufferedInputStream bis = new BufferedInputStream(is)) {
                    try (OutputStream os = Files.newOutputStream(dest.toPath())) {
                        byte[] chunk = new byte[1024];
                        int count;
                        while ((count = bis.read(chunk)) != -1) {
                            os.write(chunk, 0, count);
                        }
                        os.flush();
                    }
                }
            }
        }
        return dest.exists();
    }

    /**
     * 文件上传
     *
     * @param file 要上传的文件
     * @return 上传文件的（相对）路径
     * @throws IOException
     */
    public String fileUpload(File file) throws IOException {
        // 1.获取上传参数
        JSONObject res = (JSONObject) get("file/upload", Collections.singletonMap("file", file.getName()));
        JSONObject data = Objects.requireNonNull(res.getJSONObject("data"), "Bad result : " + res);

        // 2.1.七牛存储
        String uploadKey = data.getString("upload_key");
        String uploadToken = data.getString("upload_token");
        if (uploadToken != null) {
            if (qiniuUpload(file, uploadKey, uploadToken)) {
                return uploadKey;
            }
        }

        // 2.2.本地存储
        String uploadUrl = data.getString("upload_url");

        MediaType mediaType = MediaType.parse("multipart/form-data");
        MultipartBody multipartBody = new MultipartBody.Builder()
                .addFormDataPart("file1", file.getName(), RequestBody.create(file, mediaType))
                .build();

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(multipartBody)
                .build();

        String filePath = null;

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String resp = Objects.requireNonNull(response.body()).string();
                res = JSON.parseObject(resp);
                filePath = res.getString("data");
            } else {
                LOG.error("Upload file error : " + file);
            }
        }
        return filePath;
    }

    // TODO 请参考七牛 SDK 实现 https://developer.qiniu.com/sdk#official-sdk
    private boolean qiniuUpload(File file, String uploadKey, String uploadToken) {
        throw new UnsupportedOperationException("Please use Qiniu SDK : https://developer.qiniu.com/sdk#official-sdk");
    }
}
