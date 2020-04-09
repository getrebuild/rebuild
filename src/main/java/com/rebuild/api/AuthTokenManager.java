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
 * 授权 Token 统一管理
 *
 * @author devezhao
 * @since 2020/3/11
 */
public class AuthTokenManager {

    // Token 存储前缀
    private static final String TOKEN_PREFIX = "RBLT.";

    /**
     * Token 默认有效期（2 小时）
     */
    public static final int TOKEN_EXPIRES = 60 * 60 * 2;

    /**
     * 生成并存储 Token
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
     * 验证 Token
     *
     * @param token
     * @param verifyAfterDestroy
     * @return
     */
    public static ID verifyToken(String token, boolean verifyAfterDestroy) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        token = TOKEN_PREFIX + token;
        ID user = (ID) Application.getCommonCache().getx(token);
        if (user != null && verifyAfterDestroy) {
            Application.getCommonCache().evict(token);
        }
        return user;
    }

    /**
     * 刷新 Token 延长有效期
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
