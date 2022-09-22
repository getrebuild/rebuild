/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.user;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.api.ApiContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2019/10/25
 */
public class LoginTokenTest extends TestSupport {

    @Test
    void execute() {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("user", "rebuild");
        reqParams.put("password", "rebuild");
        ApiContext apiContext = new ApiContext(reqParams, null);

        JSONObject ret = (JSONObject) new LoginToken().execute(apiContext);
        System.out.println(ret);
        Assertions.assertEquals(ret.getIntValue("error_code"), 0);

        String loginToken = ret.getJSONObject("data").getString("login_token");
        Assertions.assertNotNull(AuthTokenManager.verifyToken(loginToken, false));
        Assertions.assertNull(AuthTokenManager.verifyToken(loginToken, false));
    }

    @Test
    void testRateLimiter() throws Exception {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("user", "rebuild");
        reqParams.put("password", "wrongpassword");
        ApiContext apiContext = new ApiContext(reqParams, null);

        for (int i = 0; i < 30; i++) {
            JSONObject ret = (JSONObject) new LoginToken().execute(apiContext);
            if (i == 20) {
                System.out.println("Waiting 30s ...");
                Thread.sleep(30 * 1000);
            }
            System.out.println("#" + i + " " + ret);
        }
    }
}