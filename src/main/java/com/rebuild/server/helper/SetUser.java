/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;

/**
 * Set call user
 *
 * @author ZHAO
 * @since 2019/11/19
 */
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
