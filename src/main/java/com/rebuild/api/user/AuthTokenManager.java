/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.user;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

/**
 * 授权（登录） Token 统一管理
 *
 * @author devezhao
 * @since 2020/3/11
 */
@Slf4j
public class AuthTokenManager {

    // Token 存储前缀
    private static final String TOKEN_PREFIX = "RBLT.";

    /**
     * Token 默认有效期（2 小时）
     */
    public static final int TOKEN_EXPIRES = 60 * 60 * 2;
    /**
     * H5 Token 默认有效期（12 小时）
     */
    public static final int H5TOKEN_EXPIRES = 60 * 60 * 12;

    /**
     * 生成并存储 Token
     *
     * @param user
     * @param expires seconds
     * @return
     */
    public static String generateToken(ID user, int expires) {
        String token = String.format("%s,%d,%s,v1",
                user, System.currentTimeMillis(), CodecUtils.randomCode(10));
        token = CodecUtils.base64UrlEncode(token);  // 64bit
        Application.getCommonsCache().putx(TOKEN_PREFIX + token, user, expires);
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
        if (StringUtils.isBlank(token)) return null;

        token = TOKEN_PREFIX + token;
        ID user = (ID) Application.getCommonsCache().getx(token);
        if (user != null && verifyAfterDestroy) {
            log.warn("Destroy token : {}", token);
            Application.getCommonsCache().evict(token);
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
        if (user == null)  return null;

        Application.getCommonsCache().putx(TOKEN_PREFIX + token, user, expires);
        return user;
    }
}
