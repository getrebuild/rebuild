/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.rebuild.core.Application;
import com.rebuild.core.support.RebuildConfiguration;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.springframework.http.HttpHeaders;

import java.io.*;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 调用工具
 *
 * @author devezhao
 * @see org.springframework.http.HttpStatus
 * @see org.springframework.http.HttpHeaders
 * @since 2020/7/15
 */
public class HttpUtils {

    private static OkHttpClient okHttpClient = null;

    private static final String RB_UA =
            String.format("RB/%s (%s/%s)", Application.VER, SystemUtils.OS_NAME, SystemUtils.JAVA_SPECIFICATION_VERSION);

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
                    .build();
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
        Request.Builder builder = new Request.Builder().url(url);
        Request request = useHeaders(builder, headers).build();

        try (Response response = getHttpClient().newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        }
    }

    /**
     * POST
     *
     * @param url
     * @param formData
     * @return
     * @throws IOException
     */
    public static String post(String url, Map<String, Object> formData) throws IOException {
        return post(url, formData, null);
    }

    /**
     * POST with Headers
     *
     * @param url
     * @param formData
     * @param headers
     * @return
     * @throws IOException
     */
    public static String post(String url, Map<String, Object> formData, Map<String, String> headers) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (formData != null && !formData.isEmpty()) {
            for (Map.Entry<String, Object> e : formData.entrySet()) {
                Object v = e.getValue();
                formBuilder.add(e.getKey(), v == null ? StringUtils.EMPTY : v.toString());
            }
        }

        Request.Builder builder = new Request.Builder().url(url);
        Request request = useHeaders(builder, headers)
                .post(formBuilder.build())
                .build();

        try (Response response = getHttpClient().newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
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
        Request.Builder builder = new Request.Builder().url(url);
        Request request = useHeaders(builder, headers).build();

        try (Response response = getHttpClient().newCall(request).execute()) {
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

        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                builder.addHeader(e.getKey(), e.getValue());
            }
        }
        return builder;
    }
}
