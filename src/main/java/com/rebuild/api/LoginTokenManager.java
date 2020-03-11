/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import org.apache.commons.lang.StringUtils;

/**
 * 认证 token 统一管理
 *
 * @author devezhao
 * @since 2020/3/11
 */
public class LoginTokenManager {

    // Token 存储前缀
    private static final String TOKEN_PREFIX = "RBLT.";

    /**
     * 生成并存储 token
     *
     * @param user
     * @param expires
     * @return
     */
    public static String generateToken(ID user, int expires) {
        String token = String.format("%s,%d,v1", user, System.currentTimeMillis());
        token = CodecUtils.base64UrlEncode(token);
        Application.getCommonCache().putx(TOKEN_PREFIX + token, user, expires);
        return token;
    }

    /**
     * 验证 token
     *
     * @param token
     * @param destroy
     * @return
     */
    public static ID verifyToken(String token, boolean destroy) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        token = TOKEN_PREFIX + token;
        ID user = (ID) Application.getCommonCache().getx(token);
        if (user != null && destroy) {
            Application.getCommonCache().evict(token);
        }
        return user;
    }

    /**
     * 刷新 token
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
}
