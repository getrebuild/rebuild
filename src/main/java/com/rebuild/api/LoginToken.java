/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.server.service.bizz.privileges.ZeroEntry;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RequestFrequencyCounter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 单点登录，获取登录 token
 *
 * @author devezhao
 * @since 2019/10/25
 *
 * @see com.rebuild.web.user.signin.LoginControll#userLogin(HttpServletRequest, HttpServletResponse)
 */
public class LoginToken extends BaseApi {

    private static final int TOKEN_EXPIRES = 60;

    private static final RequestFrequencyCounter COUNTER = new RequestFrequencyCounter();

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        String user = context.getParameterNotBlank("user");
        String password = context.getParameterNotBlank("password");

        if (COUNTER.counter(user).add().seconds(30).than(3)) {
            return formatFailure("超出请求频率", ApiInvokeException.ERR_FREQUENCY);
        }

        String hasError = checkUser(user, password);
        if (hasError != null) {
            return formatFailure(hasError);
        }

        User loginUser = Application.getUserStore().getUser(user);
        String loginToken = String.format("%s,%d,%s,v1",
                loginUser.getId(), System.currentTimeMillis(), CodecUtils.randomCode(20));
        loginToken = CodecUtils.base64UrlEncode(loginToken);
        Application.getCommonCache().putx(loginToken, loginUser.getId(), TOKEN_EXPIRES);

        JSON ret = JSONUtils.toJSONObject(
                new String[] { "login_token", "login_url" },
                new String[] { loginToken, SysConfiguration.getHomeUrl("user/login") });
        return formatSuccess(ret);
    }

    /**
     * 验证登录 token
     *
     * @param loginToken
     * @return
     */
    public static ID verifyToken(String loginToken) {
        return (ID) Application.getCommonCache().getx(loginToken);
    }

    /**
     * 检查用户
     *
     * @param user
     * @param password
     * @return
     */
    public static String checkUser(String user, String password) {
        if (!Application.getUserStore().exists(user)) {
            return "用户名或密码错误";
        }

        User loginUser = Application.getUserStore().getUser(user);
        if (!loginUser.isActive()
                || !Application.getSecurityManager().allowed(loginUser.getId(), ZeroEntry.AllowLogin)) {
            return "用户未激活或不允许登录";
        }

        Object[] foundUser = Application.createQueryNoFilter(
                "select password from User where loginName = ? or email = ?")
                .setParameter(1, user)
                .setParameter(2, user)
                .unique();
        if (foundUser == null
                || !foundUser[0].equals(EncryptUtils.toSHA256Hex(password))) {
            return "用户名或密码错误";
        }

        return null;
    }
}
