/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.language.Languages;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.server.service.bizz.privileges.ZeroEntry;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RequestFrequencyCounter;

/**
 * 单点登录，获取登录 token
 *
 * @author devezhao
 * @since 2019/10/25
 */
public class LoginToken extends BaseApi {

    // Token 存储前缀
    private static final String TOKEN_PREFIX = "RBLT.";

    private static final RequestFrequencyCounter COUNTER = new RequestFrequencyCounter();

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        String user = context.getParameterNotBlank("user");
        String password = context.getParameterNotBlank("password");

        if (COUNTER.counter(user).add().seconds(30).than(3)) {
            return formatFailure("Request frequency exceeded", ApiInvokeException.ERR_FREQUENCY);
        }

        String hasError = checkUser(user, password);
        if (hasError != null) {
            return formatFailure(hasError);
        }

        User loginUser = Application.getUserStore().getUser(user);
        String loginToken = generateToken(loginUser.getId(), 60);

        JSON ret = JSONUtils.toJSONObject(
                new String[] { "login_token", "login_url" },
                new String[] { loginToken, SysConfiguration.getHomeUrl("user/login") });
        return formatSuccess(ret);
    }

    // --

    /**
     * 生成并存储 Token
     *
     * @param user
     * @param expires
     * @return
     */
    public static String generateToken(ID user, int expires) {
        String token = String.format("%s,%d,%s,v1", user, System.currentTimeMillis(), CodecUtils.randomCode(20));
        token = CodecUtils.base64UrlEncode(token);
        Application.getCommonCache().putx(TOKEN_PREFIX + token, user, expires);
        return token;
    }

    /**
     * 验证 Token
     *
     * @param token
     * @return
     */
    public static ID verifyToken(String token, boolean destroy) {
        token = TOKEN_PREFIX + token;
        ID user = (ID) Application.getCommonCache().getx(token);
        if (user != null && destroy) {
            Application.getCommonCache().evict(token);
        }
        return user;
    }

    /**
     * 刷新 Token
     *
     * @param token
     * @param expires
     * @return
     */
    public static ID refreshToken(String token, int expires) {
        ID user = verifyToken(token, false);
        if (user == null) {
            return null;
        }

        Application.getCommonCache().putx(TOKEN_PREFIX + token, user, expires);
        return user;
    }

    /**
     * 检查用户
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
