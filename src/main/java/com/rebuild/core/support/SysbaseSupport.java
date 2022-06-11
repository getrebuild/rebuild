/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.FileAppender;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.slf4j.LoggerFactory;

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
        File file = getLogbackFile();

        JSONObject resJson;
        try {
            String res = upload(file, "http://localhost:3000/api/misc/request-support");
            log.info("Upload support file : {}", res);
            resJson = (JSONObject) JSON.parse(res);
        } catch (IOException e) {
            log.error("Upload support file failed", e);
            return null;
        }

        return resJson.getString("TSID");
    }

    /**
     * 最新日志文件
     *
     * @return
     */
    public File getLogbackFile() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        @SuppressWarnings("rawtypes")
        FileAppender fa = (FileAppender) lc.getLogger("ROOT").getAppender("FILE");
        return new File(fa.getFile());
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
