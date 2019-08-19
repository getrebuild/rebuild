/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.recyclebin;

import cn.devezhao.persist4j.engine.ID;

/**
 * 数据恢复
 *
 * @author devezhao
 * @since 2019/8/19
 */
public class RecycleRestore {

    final private ID recycleId;

    public RecycleRestore(ID recycleId) {
        this.recycleId = recycleId;
    }

    /**
     * 恢复数据
     *
     * @param cascade 级联恢复
     * @return
     */
    public int restore(boolean cascade) {
        return 0;
    }
}
