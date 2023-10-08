/*!
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
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;

/**
 * 获取登录 Token 可用于单点登录
 *
 * @author devezhao
 * @since 2019/10/25
 */
public class LoginToken extends BaseApi {

    // 基于用户限流
    private static final RequestRateLimiter RRL_4USER = RateLimiters.createRateLimiter(
            new int[] { 60, 600, 3600 },
            new int[] { 5, 15, 30 });

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        final String user = context.getParameterNotBlank("user");
        final String password = context.getParameterNotBlank("password");

        if (RRL_4USER.overLimitWhenIncremented("user:" + user)) {
            return formatFailure(Language.L("请求过于频繁，请稍后重试"), ApiInvokeException.ERR_FREQUENCY);
        }

        String hasError = checkUser(user, password);
        if (hasError != null) {
            return formatFailure(hasError);
        }

        User loginUser = Application.getUserStore().getUser(user);
        String loginToken = AuthTokenManager.generateOnceToken(loginUser.getId());

        JSON ret = JSONUtils.toJSONObject(
                new String[]{"login_token", "login_url"},
                new String[]{loginToken, RebuildConfiguration.getHomeUrl("user/login?token=" + loginToken)});
        return formatSuccess(ret);
    }

    // --

    /**
     * 检查用户登录
     *
     * @param user
     * @param password
     * @return 返回 null 表示成功
     */
    public static String checkUser(String user, String password) {
        if (!Application.getUserStore().existsUser(user)) {
            return Language.L("用户名或密码错误");
        }

        User loginUser = Application.getUserStore().getUser(user);
        if (!loginUser.isActive()
                || !Application.getPrivilegesManager().allow(loginUser.getId(), ZeroEntry.AllowLogin)) {
            return Language.L("用户未激活或不允许登录");
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
            return Language.L("用户名或密码错误");
        }
    }
}
