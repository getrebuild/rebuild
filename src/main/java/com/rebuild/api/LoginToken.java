/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import cn.devezhao.commons.EncryptUtils;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.language.Languages;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.server.service.bizz.privileges.ZeroEntry;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RateLimiters;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;

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
            return formatFailure("请求太频繁，请稍后重试", ApiInvokeException.ERR_FREQUENCY);
        }

        String hasError = checkUser(user, password);
        if (hasError != null) {
            return formatFailure(hasError);
        }

        User loginUser = Application.getUserStore().getUser(user);
        String loginToken = AuthTokenManager.generateToken(loginUser.getId(), 60);

        JSON ret = JSONUtils.toJSONObject(
                new String[] { "login_token", "login_url" },
                new String[] { loginToken, SysConfiguration.getHomeUrl("user/login") });
        return formatSuccess(ret);
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
            return Languages.lang("InputWrong", "UsernameOrPassword");
        }

        User loginUser = Application.getUserStore().getUser(user);
        if (!loginUser.isActive()
                || !Application.getSecurityManager().allow(loginUser.getId(), ZeroEntry.AllowLogin)) {
            return Languages.lang("UnactiveUserTip");
        }

        Object[] foundUser = Application.createQueryNoFilter(
                "select password from User where loginName = ? or email = ?")
                .setParameter(1, user)
                .setParameter(2, user)
                .unique();
        if (foundUser == null
                || !foundUser[0].equals(EncryptUtils.toSHA256Hex(password))) {
            return Languages.lang("InputWrong", "UsernameOrPassword");
        }

        return null;
    }
}
