/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.identifier.ComputerIdentifier;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.springframework.http.HttpHeaders;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * okhttp3 调用封装
 *
 * @author devezhao
 * @see org.springframework.http.HttpStatus
 * @see HttpHeaders
 * @since 2020/7/15
 */
@Slf4j
public class HttpUtils {

    private static OkHttpClient okHttpClient = null;

    public static final String RB_UA = String.format("RB/%s (%s/%s)",
            Application.VER, SystemUtils.OS_NAME, SystemUtils.JAVA_SPECIFICATION_VERSION);

    private static final Locale l = Locale.getDefault();
    public static final String RB_LANG = l.getLanguage() + "_" + l.getCountry();

    private static String RB_CI;

    /**
     * 获取客户端，如本类提供的 GET/POST 方法无法满足需求，可以自己构建然后通过此 Client 调用。
     * 注意不要修改此 Client 的默认行为，如果需要修改应该自己构建一个 Client
     *
     * @return
     */
    synchronized public static OkHttpClient getHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .hostnameVerifier((s, sslSession) -> true)  // NOT SAFE!!!
                    .build();
            RB_CI = ComputerIdentifier.generateIdentifierKey();
        }
        return okHttpClient;
    }

    /**
     * GET
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static String get(String url) throws IOException {
        return get(url, null);
    }

    /**
     * GET with Headers
     *
     * @param url
     * @param headers
     * @return
     * @throws IOException
     */
    public static String get(String url, Map<String, String> headers) throws IOException {
        OkHttpClient client = getHttpClient();
        Request.Builder builder = new Request.Builder().url(url);
        Request request = useHeaders(builder, headers).build();

        long ms = System.currentTimeMillis();
        try (Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        } finally {
            ms = System.currentTimeMillis() - ms;
            if (ms > 3000) log.warn("Http GET `{}` time {}ms", url, ms);
        }
    }

    /**
     * POST
     *
     * @param url
     * @param reqData
     * @return
     * @throws IOException
     */
    public static String post(String url, Object reqData) throws IOException {
        return post(url, reqData, null);
    }

    /**
     * POST with Headers
     *
     * @param url
     * @param reqData
     * @param headers
     * @return
     * @throws IOException
     */
    public static String post(String url, Object reqData, Map<String, String> headers) throws IOException {
        RequestBody requestBody;

        // JSON
        if (reqData instanceof JSON) {
            requestBody = RequestBody.create(((JSON) reqData).toJSONString(), MediaType.parse("application/json"));
        }
        // Map
        else if (reqData instanceof Map) {
            FormBody.Builder formBuilder = new FormBody.Builder();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) reqData).entrySet()) {
                Object v = e.getValue();
                formBuilder.add(e.getKey().toString(), v == null ? StringUtils.EMPTY : v.toString());
            }
            requestBody = formBuilder.build();
        }
        // Text
        else {
            requestBody = RequestBody.create(reqData == null ? "" : reqData.toString(), MediaType.parse("text/plain"));
        }

        OkHttpClient client = getHttpClient();
        Request.Builder builder = new Request.Builder().url(url);
        Request request = useHeaders(builder, headers)
                .post(requestBody)
                .build();

        long ms = System.currentTimeMillis();
        try (Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        } finally {
            ms = System.currentTimeMillis() - ms;
            if (ms > 3000) log.warn("Http POST `{}` time {}ms", url, ms);
        }
    }

    /**
     * GET binary into file
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static File readBinary(String url) throws IOException {
        File tmp = RebuildConfiguration.getFileOfTemp("download." + UUID.randomUUID());
        boolean success = readBinary(url, tmp, Collections.singletonMap(HttpHeaders.USER_AGENT, RB_UA));
        return success && tmp.exists() ? tmp : null;
    }

    /**
     * GET binary with Headers
     *
     * @param url
     * @param dest
     * @param headers
     * @return
     * @throws IOException
     */
    public static boolean readBinary(String url, File dest, Map<String, String> headers) throws IOException {
        OkHttpClient client = getHttpClient();
        Request.Builder builder = new Request.Builder().url(url);
        Request request = useHeaders(builder, headers).build();

        try (Response response = client.newCall(request).execute()) {
            try (InputStream is = Objects.requireNonNull(response.body()).byteStream()) {
                try (BufferedInputStream bis = new BufferedInputStream(is)) {
                    try (OutputStream os = new FileOutputStream(dest)) {
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
        return true;
    }

    private static Request.Builder useHeaders(Request.Builder builder, Map<String, String> headers) {
        builder.addHeader(HttpHeaders.USER_AGENT, RB_UA);
        builder.addHeader(HttpHeaders.ACCEPT_LANGUAGE, RB_LANG);
        if (RB_CI != null) builder.addHeader("X-RB-CI", RB_CI);

        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                builder.addHeader(e.getKey(), e.getValue());
            }
        }
        return builder;
    }
}
