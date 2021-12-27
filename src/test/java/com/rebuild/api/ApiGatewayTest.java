/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSON;
import com.rebuild.TestSupport;
import com.rebuild.api.sdk.OpenApiSDK;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.RebuildApiManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.RecordBuilder;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author devezhao
 * @since 2019/7/23
 */
public class ApiGatewayTest extends TestSupport {

    @Disabled
    @Test
    void testOpenApiSDK() {
        final String[] app = createApp();
        final String baseUrl = "http://localhost:18080/rebuild/gw/api/";

        // 加密签名请求
        OpenApiSDK openApiSDK = new OpenApiSDK(app[0], app[1], baseUrl);
        JSON ret = openApiSDK.get("system-time", null);
        System.out.println("OpenApiSDK response : " + ret);

        // 明文请求
        String apiUrl = baseUrl + "page-token-verify?appid=" + app[0] + "&sign=" + app[1] + "&token=123";
        try {
            ret = openApiSDK.httpGet(apiUrl);
            System.out.println("OpenApiSDK response (plaintext) : " + ret);

        } catch (IOException ignored) {
        }
    }

    /**
     * 创建用于调用接口的 API Key
     *
     * @return
     */
    protected static String[] createApp() {
        final String appId = "999999999";
        ConfigBean exists = RebuildApiManager.instance.getApp(appId);
        if (exists != null) {
            return new String[]{appId, exists.getString("appSecret")};
        }

        String appSecret = CodecUtils.randomCode(40);
        Record record = RecordBuilder.builder(EntityHelper.RebuildApi)
                .add("appId", appId)
                .add("appSecret", appSecret)
                .add("bindUser", UserService.SYSTEM_USER)
                .build(UserService.SYSTEM_USER);
        Application.getCommonsService().create(record, false);

        RebuildApiManager.instance.clean(appId);
        return new String[] { appId, appSecret };
    }
}
