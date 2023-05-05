/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.user;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.api.BaseApi;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;

/**
 * 页面 Token 验证
 *
 * @author devezhao
 * @since 2021/10/19
 */
public class PageTokenVerify extends BaseApi {

    public static final int TOKEN_EXPIRES = CommonsCache.TS_HOUR * 2;

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        String token = context.getParameterNotBlank("token");
        ID tokenUser = verify(token);
        if (tokenUser == null) {
            throw new ApiInvokeException("Invalid token : " + token);
        }

        User user = Application.getUserStore().getUser(tokenUser);
        JSON ret = JSONUtils.toJSONObject(
                new String[]{"user_id", "login_name", "full_name"},
                new Object[]{user.getId(), user.getName(), user.getFullName()});
        return formatSuccess(ret);
    }

    // -- TOKENs

    /**
     * 生成 Token，有效期 2h，如期间调用 #verify 会续期 2h
     *
     * @param user
     * @return
     */
    public static String generate(ID user) {
        String ptoken = CommonsUtils.randomHex(true);
        Application.getCommonsCache().putx("RBPT." + ptoken, user, TOKEN_EXPIRES);
        return ptoken;
    }

    /**
     * 验证 Token
     *
     * @param ptoken
     * @return
     */
    protected static ID verify(String ptoken) {
        ID user = (ID) Application.getCommonsCache().getx("RBPT." + ptoken);
        if (user == null) return null;

        // 自动续期
        Application.getCommonsCache().putx("RBPT." + ptoken, user, TOKEN_EXPIRES);
        return user;
    }
}
