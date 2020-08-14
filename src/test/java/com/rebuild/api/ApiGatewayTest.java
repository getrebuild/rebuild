/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.sdk.OpenApiSDK;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.RebuildApiManager;
import com.rebuild.server.helper.FormDataBuilder;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.web.MvcResponse;
import com.rebuild.web.TestSupportWithMVC;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2019/7/23
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ApiGatewayTest extends TestSupportWithMVC {

    @Test
    public void testSimple() throws Exception {
        final String[] app = createApp();

        String apiUrl = "/gw/api/system-time?";
        Map<String, Object> bizParams = new HashMap<>();

        apiUrl += new OpenApiSDK(app[0], app[1]).sign(bizParams, "SHA1");
        System.out.println("Request API : " + apiUrl);

        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(apiUrl);
        MvcResponse resp = perform(builder, null);
        System.out.println(resp);
    }

    @Ignore
    @Test
    public void testOpenApiSDK() {
        final String[] app = createApp();
        final String baseUrl = "http://localhost:8180/rebuild/gw/api/";

        // 加密签名请求
        OpenApiSDK openApiSDK = new OpenApiSDK(app[0], app[1], baseUrl);
        JSON ret = openApiSDK.get("system-time", null);
        System.out.println("OpenApiSDK response : " + ret);

        // 明文请求
        String apiUrl = baseUrl + "system-time?appid=" + app[0] + "&sign=" + app[1];
        try {
            ret = openApiSDK.httpGet(apiUrl);
            System.out.println("OpenApiSDK response (plaintext) : " + ret);

        } catch (IOException ignored) {
        }
    }

    /**
     * @return
     */
    protected static String[] createApp() {
        final String appId = "999999999";
        ConfigEntry exists = RebuildApiManager.instance.getApp(appId);
        if (exists != null) {
            return new String[] { appId, exists.getString("appSecret") };
        }

        String appSecret = CodecUtils.randomCode(40);
        Record record = FormDataBuilder.builder(EntityHelper.RebuildApi)
                .add("appId", appId)
                .add("appSecret", appSecret)
                .add("bindUser", UserService.SYSTEM_USER)
                .buildRecord(UserService.SYSTEM_USER);
        Application.getCommonsService().create(record, false);

        RebuildApiManager.instance.clean(appId);
        return new String[] { appId, appSecret };
    }
}
