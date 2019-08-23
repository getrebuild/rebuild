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

import java.util.UUID;

/**
 * 授权许可
 *
 * @author ZHAO
 * @since 2019-08-23
 */
public final class Lisence {

    /**
     * SN 码
     *
     * @return
     */
    public static String SN() {
        String SN = SysConfiguration.get(ConfigurableItem.SN);
        if (SN == null) {
            SN = "OS" + UUID.randomUUID().toString().substring(9, 35).toUpperCase();
            SysConfiguration.set(ConfigurableItem.SN, SN);
        }
        return SN;
    }
}
