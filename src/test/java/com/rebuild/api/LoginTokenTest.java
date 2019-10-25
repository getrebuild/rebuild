/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.api;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.TestSupport;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2019/10/25
 */
public class LoginTokenTest extends TestSupport {

    @Test
    public void execute() {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("user", "rebuild");
        reqParams.put("password", "rebuild");
        ApiContext apiContext = new ApiContext(reqParams, null);

        JSONObject ret = (JSONObject) new LoginToken().execute(apiContext);
        System.out.println(ret);

        String loginToken = ret.getJSONObject("data").getString("login_token");
        Assert.assertTrue(LoginToken.verifyToken(loginToken) != null);
    }

    @Test
    public void frequency() {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("user", "rebuild");
        reqParams.put("password", "wrongpassword");
        ApiContext apiContext = new ApiContext(reqParams, null);

        for (int i = 0; i < 5; i++) {
            JSONObject ret = (JSONObject) new LoginToken().execute(apiContext);
            System.out.println(ret);
        }
    }
}