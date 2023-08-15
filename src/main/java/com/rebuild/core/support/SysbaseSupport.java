/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * @author RB
 * @since 2022/6/8
 */
@Slf4j
public class SysbaseSupport {

    /**
     * 提交支持
     *
     * @return
     */
    public String submit() {
        File file = SysbaseHeartbeat.getLogbackFile();

        JSONObject resJson;
        try {
            String res = upload(file, "https://getrebuild.com/api/misc/request-support");
            log.info("Upload support-file : {}", res);
            resJson = (JSONObject) JSON.parse(res);
        } catch (IOException e) {
            log.error("Upload support-file failed", e);
            return null;
        }

        return resJson.getString("TSID");
    }

    /**
     * @param file
     * @param uploadUrl
     * @return
     * @throws IOException
     */
    protected String upload(File file, String uploadUrl) throws IOException {
        OkHttpClient client = OkHttpUtils.getHttpClient();

        RequestBody fileBody = RequestBody.create(
                file, MediaType.parse("multipart/form-data"));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request.Builder builder = new Request.Builder()
                .url(uploadUrl)
                .post(requestBody);
        Request request = OkHttpUtils.useHeaders(builder, null).build();

        try (Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        }
    }
}
