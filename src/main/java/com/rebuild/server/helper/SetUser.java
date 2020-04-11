/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;

/**
 * 设置执行用户，如果未设置则使用当前线程用户。
 * 一般在 web.Controll 中调用无需指定用户，因为 web 拦截器已经设置了当前线程用户。
 *
 * @author ZHAO
 * @since 2019/11/19
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class SetUser<T extends SetUser> {

    private ID user;

    /**
     * 设置用户
     *
     * @param user
     * @return
     */
    public T setUser(ID user) {
        this.user = user;
        return (T) this;
    }

    /**
     * 获取用户，未设置则返回当前线程用户
     *
     * @return
     */
    public ID getUser() {
        return user != null ? user : Application.getCurrentUser();
    }
}
