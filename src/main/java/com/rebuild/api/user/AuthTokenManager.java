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
import com.rebuild.utils.AppUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;

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
     * 不可刷新、有效期 30m
     */
    public static final String TYPE_CSRF_TOKEN = "RBCT";

    /**
     * 不可刷新、有效期 1m、一次性使用
     */
    public static final String TYPE_ONCE_TOKEN = "RBOT";

    /**
     * H5 Token 默认有效期（12 小时）
     */
    public static final int H5TOKEN_EXPIRES = CommonsCache.TS_HOUR * 12;

    private static final String TOKEN_PREFIX = "TOKEN:";

    /**
     * 生成 Token
     *
     * @param user
     * @param expires
     * @param type
     * @return
     */
    public static String generateToken(ID user, int expires, String type) {
        // Type,User,Time
        String desc = String.format("%s,%s,%d,v2",
                ObjectUtils.defaultIfNull(type, TYPE_ACCESS_TOKEN),
                ObjectUtils.defaultIfNull(user, UserService.SYSTEM_USER),
                System.nanoTime());
        String token = EncryptUtils.toMD5Hex(desc);

        Application.getCommonsCache().putx(TOKEN_PREFIX + token, desc, expires);
        return token;
    }

    /**
     * @param user
     * @param expires
     * @return
     * @see #TYPE_ACCESS_TOKEN
     */
    public static String generateAccessToken(ID user, int expires) {
        Assert.notNull(user, "[user] cannot be null");
        return generateToken(user, expires, TYPE_ACCESS_TOKEN);
    }

    /**
     * @return
     * @see #TYPE_CSRF_TOKEN
     */
    public static String generateCsrfToken() {
        return generateToken(null, CommonsCache.TS_MINTE * 30, TYPE_CSRF_TOKEN);
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
     * @param request
     * @param verifyAfterDestroy
     * @return
     * @see #TYPE_CSRF_TOKEN
     */
    public static boolean verifyCsrfToken(HttpServletRequest request, boolean verifyAfterDestroy) {
        String csrfToken = request.getHeader(AppUtils.HF_CSRFTOKEN);
        if (csrfToken == null) csrfToken = request.getParameter(AppUtils.URL_CSRFTOKEN);
        return verifyToken(csrfToken, verifyAfterDestroy) != null;
    }

    /**
     * 刷新 AccessToken 延长有效期
     *
     * @param token
     * @param expires
     * @return
     */
    public static ID refreshAccessToken(String token, int expires) {
        Assert.notNull(token, "[token] cannot be null");
        String desc = Application.getCommonsCache().get(TOKEN_PREFIX + token);
        if (desc == null) return null;

        String[] descs = desc.split(",");
        Assert.isTrue(TYPE_ACCESS_TOKEN.equals(descs[0]), "Cannot refresh none access token");

        Application.getCommonsCache().put(TOKEN_PREFIX + token, desc, expires);
        return ID.valueOf(descs[1]);
    }
}
