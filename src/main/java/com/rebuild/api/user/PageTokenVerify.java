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
import com.rebuild.utils.JSONUtils;

import java.util.UUID;

/**
 * 页面 Token 验证
 *
 * @author devezhao
 * @since 2021/10/19
 */
public class PageTokenVerify extends BaseApi {

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
     * @param user
     * @return
     */
    public static String generate(ID user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Application.getCommonsCache().putx("RBPT." + token, user, CommonsCache.TS_HOUR);
        return token;
    }

    /**
     * @param token
     * @return
     */
    protected static ID verify(String token) {
        return (ID) Application.getCommonsCache().getx("RBPT." + token);
    }
}
