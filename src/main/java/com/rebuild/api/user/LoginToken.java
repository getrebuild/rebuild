/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.user;

import cn.devezhao.commons.EncryptUtils;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.api.BaseApi;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RateLimiters;

/**
 * 获取登录 Token 可用于单点登录
 *
 * @author devezhao
 * @since 2019/10/25
 */
public class LoginToken extends BaseApi {

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        String user = context.getParameterNotBlank("user");
        String password = context.getParameterNotBlank("password");

        if (RateLimiters.RRL_LOGIN.overLimitWhenIncremented("user:" + user)) {
            return formatFailure(Language.L("RequestsTooFrequent"), ApiInvokeException.ERR_FREQUENCY);
        }

        String hasError = checkUser(user, password);
        if (hasError != null) {
            return formatFailure(hasError);
        }

        User loginUser = Application.getUserStore().getUser(user);
        String loginToken = AuthTokenManager.generateToken(loginUser.getId(), 60);

        JSON data = JSONUtils.toJSONObject(
                new String[] { "login_token", "login_url" },
                new String[] { loginToken, RebuildConfiguration.getHomeUrl("user/login") });
        return formatSuccess(data);
    }

    // --

    /**
     * 检查用户登录
     *
     * @param user
     * @param password
     * @return
     */
    public static String checkUser(String user, String password) {
        if (!Application.getUserStore().existsUser(user)) {
            return Language.L("SomeError", "UsernameOrPassword");
        }

        User loginUser = Application.getUserStore().getUser(user);
        if (!loginUser.isActive()
                || !Application.getPrivilegesManager().allow(loginUser.getId(), ZeroEntry.AllowLogin)) {
            return Language.L("UnactiveUser");
        }

        Object[] foundUser = Application.createQueryNoFilter(
                "select password from User where loginName = ? or email = ?")
                .setParameter(1, user)
                .setParameter(2, user)
                .unique();
        if (foundUser != null && foundUser[0].equals(EncryptUtils.toSHA256Hex(password))) {
            // Okay
            return null;
        } else {
            return Language.L("SomeError", "UsernameOrPassword");
        }
    }
}
