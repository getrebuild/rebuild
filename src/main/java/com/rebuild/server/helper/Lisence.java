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

import com.alibaba.fastjson.JSON;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Locale;
import java.util.UUID;

/**
 * 授权许可
 *
 * @author ZHAO
 * @since 2019-08-23
 */
public final class Lisence {

    /**
     * 授权码/SN
     *
     * @return
     */
    public static String SN() {
        String SN = SysConfiguration.get(ConfigurableItem.SN, true);
        if (SN == null) {
            SN = String.format("ZR%s%s-%s",
                    "107",
                    StringUtils.leftPad(Locale.getDefault().getCountry(), 3, "0"),
                    UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase());
            SysConfiguration.set(ConfigurableItem.SN, SN);
        }
        return SN;
    }

    /**
     * 查询授权信息
     *
     * @return
     */
    public static JSON queryAuthority() {
        String queryUrl = "https://getrebuild.com/authority/query?k=IjkMHgq94T7s7WkP&sn=" + SN();
        try {
            String result = CommonsUtils.get(queryUrl);
            if (StringUtils.isNotBlank(result) && JSONUtils.wellFormat(result)) {
                return JSON.parseObject(result);
            }
        } catch (Exception ignored) {
        }
        return JSONUtils.toJSONObject(new String[]{"sn", "authType"}, new String[]{SN(), "开源社区版"});
    }
}
