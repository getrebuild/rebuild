/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.lang.StringUtils;

/**
 * 授权（登录） Token 统一管理
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
     * @param expires seconds
     * @return
     */
    public static String generateToken(ID user, int expires) {
        String token = String.format("%s,%d,v1", user, System.currentTimeMillis());
        token = CodecUtils.base64UrlEncode(token);
        Application.getCommonsCache().putx(TOKEN_PREFIX + token, user, expires);
        return token;
    }

    /**
     * 验证 Token
     *
     * @param token
     * @param verifyAndDestroy
     * @return
     */
    public static ID verifyToken(String token, boolean verifyAndDestroy) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        token = TOKEN_PREFIX + token;
        ID user = (ID) Application.getCommonsCache().getx(token);
        if (user != null && verifyAndDestroy) {
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
        if (user == null) {
            return null;
        }

        Application.getCommonsCache().putx(TOKEN_PREFIX + token, user, expires);
        return user;
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
