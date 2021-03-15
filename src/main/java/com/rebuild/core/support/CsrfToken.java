/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.CodecUtils;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.utils.AppUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao
 * @since 2020/12/15
 */
public class CsrfToken {

    // Token 存储前缀
    private static final String TOKEN_PREFIX = "RBCSRF.";

    /**
     * 生成并存储 Token
     *
     * @return
     */
    public static String generate() {
        String token = CodecUtils.randomCode(60);
        Application.getCommonsCache().putx(TOKEN_PREFIX + token,
                System.currentTimeMillis(), CommonsCache.TS_HOUR * 2);
        return token;
    }

    /**
     * 验证 Token
     *
     * @param token
     * @param destroy
     * @return
     */
    public static boolean verify(String token, boolean destroy) {
        if (StringUtils.isBlank(token)) return false;

        token = TOKEN_PREFIX + token;
        Object exists = Application.getCommonsCache().getx(token);
        if (exists != null && destroy) {
            Application.getCommonsCache().evict(token);
        }
        return exists != null;
    }

    /**
     * @param request
     * @param destroy
     * @return
     */
    public static boolean verify(HttpServletRequest request, boolean destroy) {
        String token = request.getHeader(AppUtils.HF_CSRFTOKEN);
        if (token == null) token = request.getParameter("_csrfToken");
        return verify(token, destroy);
    }
}
