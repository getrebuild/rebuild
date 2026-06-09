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
import com.rebuild.utils.RebuildBanner;
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
import java.util.ArrayList;
import java.util.List;
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
        List<String> logConf = new ArrayList<>();
        logConf.add("RebuildConfiguration :");
        for (ConfigurationItem item : ConfigurationItem.values()) {
            String v = RebuildConfiguration.get(item, true);
            logConf.add(StringUtils.leftPad(item.name(), 23) + " : " + (v == null ? "" : v));
        }
        log.warn(RebuildBanner.formatSimple(true, logConf.toArray(new String[0])));

        logConf.clear();
        logConf.add("OS/VM INFO :");
        logConf.add(StringUtils.leftPad("OS", 26) + " : " + SystemUtils.OS_NAME + "/" + SystemUtils.OS_VERSION);
        logConf.add(StringUtils.leftPad("VM", 26) + " : " + SystemUtils.JAVA_VM_NAME + "/" + SystemUtils.JAVA_VERSION);
        logConf.add(StringUtils.leftPad("TimeZone", 26) + " : " + CalendarUtils.DEFAULT_TIME_ZONE);
        logConf.add(StringUtils.leftPad("Date", 26) + " : " + CalendarUtils.now());
        logConf.add(StringUtils.leftPad("VM ARGUMENTS", 26) + " : " + System.getProperties());
        log.warn(RebuildBanner.formatSimple(true, logConf.toArray(new String[0])));

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
