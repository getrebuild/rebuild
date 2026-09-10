/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.user;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.EncryptUtils;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.api.BaseApi;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RateLimiters;
import com.rebuild.web.user.signup.LoginAction;
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
            new int[]{60, 600, 3600},
            new int[]{5, 15, 30});

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        final String user = context.getParameterNotBlank("user");
        final String password = context.getParameterNotBlank("password");

        if (RRL_4USER.overLimitWhenIncremented("user:" + user)) {
            return formatFailure(Language.L("请求过于频繁，请稍后重试"), ApiInvokeException.ERR_FREQUENCY);
        }

        // 兼容处理
        String salt = "iloverb" + CalendarUtils.format("yyyyMMdd", CalendarUtils.now());
        String passwd45 = EncryptUtils.toSHA256Hex(password);
        passwd45 = EncryptUtils.toSHA256Hex(passwd45 + salt);

        String hasError = LoginAction.checkUser(user, passwd45);
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
}
