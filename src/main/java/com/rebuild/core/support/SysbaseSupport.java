/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.CalendarUtils;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

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
        StringBuilder confLog = new StringBuilder("RebuildConfiguration :\n----------\n");
        for (ConfigurationItem item : ConfigurationItem.values()) {
            String v = RebuildConfiguration.get(item);
            confLog.append(StringUtils.rightPad(item.name(), 31)).append(" : ").append(v == null ? "" : v).append("\n");
        }
        log.warn(confLog.append("----------").toString());

        StringBuilder osLog = new StringBuilder("OS/VM INFO :\n----------\n");
        osLog.append(StringUtils.rightPad("OS", 31)).append(" : ").append(SystemUtils.OS_NAME).append("/").append(SystemUtils.OS_VERSION).append("\n");
        osLog.append(StringUtils.rightPad("VM", 31)).append(" : ").append(SystemUtils.JAVA_VM_NAME).append("/").append(SystemUtils.JAVA_VERSION).append(SystemUtils.OS_VERSION).append("\n");
        osLog.append(StringUtils.rightPad("TimeZone", 31)).append(" : ").append(CalendarUtils.DEFAULT_TIME_ZONE).append("\n");
        osLog.append(StringUtils.rightPad("Date", 31)).append(" : ").append(CalendarUtils.now()).append("\n");
        osLog.append(StringUtils.rightPad("Headless", 31)).append(" : ").append(SystemUtils.isJavaAwtHeadless()).append("\n");
        log.warn(osLog.append("----------").toString());

        StringBuilder vmLog = new StringBuilder("VM ARGUMENTS :\n----------\n");
        vmLog.append(System.getProperties());
        log.warn(vmLog.append("----------").toString());

        File logFile = SysbaseHeartbeat.getLastLogbackFile(false);

        JSONObject resJson;
        try {
            String res = upload(logFile, "https://getrebuild.com/api/misc/request-support");
            log.info("Upload file of support : {}", res);
            resJson = (JSONObject) JSON.parse(res);
        } catch (IOException e) {
            log.error("Upload file of support fails", e);
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
