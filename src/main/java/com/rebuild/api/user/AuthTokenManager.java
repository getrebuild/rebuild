/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.user;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.privileges.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.util.Assert;

/**
 * Token 统一管理
 *
 * @author devezhao
 * @since 2020/3/11
 */
@Slf4j
public class AuthTokenManager {

    /**
     * 可刷新、有效期自定义
     */
    public static final String TYPE_ACCESS_TOKEN = "RBAT";

    /**
     * 不可刷新、有效期自定义
     */
    public static final String TYPE_CSRF_TOKEN = "RBCT";

    /**
     * 不可刷新、有效期 1m、一次性使用
     */
    public static final String TYPE_ONCE_TOKEN = "RBOT";

    private static final int ACCESSTOKEN_EXPIRES = CommonsCache.TS_HOUR * 12;

    private static final String TOKEN_PREFIX = "TOKEN:";

    /**
     * 生成 Token
     *
     * @param user
     * @param expires
     * @param type
     * @return
     */
    protected static String generateToken(ID user, int expires, String type) {
        // Type,User,Time,Version
        String desc = String.format("%s,%s,%d,v2",
                ObjectUtils.defaultIfNull(type, TYPE_ACCESS_TOKEN),
                ObjectUtils.defaultIfNull(user, UserService.SYSTEM_USER),
                System.nanoTime());
        String token = EncryptUtils.toSHA1Hex(desc);

        Application.getCommonsCache().putx(TOKEN_PREFIX + token, desc, expires);
        return token;
    }

    /**
     * @param user
     * @return
     * @see #TYPE_ACCESS_TOKEN
     */
    public static String generateAccessToken(ID user) {
        Assert.notNull(user, "[user] cannot be null");
        return generateToken(user, ACCESSTOKEN_EXPIRES, TYPE_ACCESS_TOKEN);
    }

    /**
     * @return
     * @see #TYPE_CSRF_TOKEN
     */
    public static String generateCsrfToken() {
        return generateCsrfToken(CommonsCache.TS_MINTE * 30);
    }

    /**
     * @param expires
     * @return
     * @see #TYPE_CSRF_TOKEN
     */
    public static String generateCsrfToken(int expires) {
        return generateToken(null, expires, TYPE_CSRF_TOKEN);
    }

    /**
     * @param user
     * @return
     * @see #TYPE_ONCE_TOKEN
     */
    public static String generateOnceToken(ID user) {
        return generateToken(user, CommonsCache.TS_MINTE, TYPE_ONCE_TOKEN);
    }

    /**
     * 验证 Token
     *
     * @param token
     * @return
     */
    public static ID verifyToken(String token) {
        return verifyToken(token, Boolean.FALSE);
    }

    /**
     * 验证 Token
     *
     * @param token
     * @param verifyAfterDestroy
     * @return
     */
    public static ID verifyToken(String token, boolean verifyAfterDestroy) {
        Assert.notNull(token, "[token] cannot be null");
        String desc = Application.getCommonsCache().get(TOKEN_PREFIX + token);
        if (desc == null) return null;

        String[] descs = desc.split(",");
        if (TYPE_ONCE_TOKEN.equals(descs[0])) {
            verifyAfterDestroy = true;
        }

        if (verifyAfterDestroy) {
            log.debug("Destroy token ({}) : {}", descs[0], token);
            Application.getCommonsCache().evict(TOKEN_PREFIX + token);
        }

        return ID.valueOf(descs[1]);
    }

    /**
     * 刷新 AccessToken 延长有效期
     *
     * @param token
     * @return
     */
    public static ID refreshAccessToken(String token) {
        Assert.notNull(token, "[token] cannot be null");
        String desc = Application.getCommonsCache().get(TOKEN_PREFIX + token);
        if (desc == null) return null;

        String[] descs = desc.split(",");
        Assert.isTrue(TYPE_ACCESS_TOKEN.equals(descs[0]), "Cannot refresh none access token");

        Application.getCommonsCache().put(TOKEN_PREFIX + token, desc, ACCESSTOKEN_EXPIRES);
        return ID.valueOf(descs[1]);
    }
}
