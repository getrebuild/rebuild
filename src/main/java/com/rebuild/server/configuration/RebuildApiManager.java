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

package com.rebuild.server.configuration;

import com.rebuild.server.Application;

/**
 * API 鉴权参数
 *
 * @author ZHAO
 * @since 2019-07-23
 */
public class RebuildApiManager implements ConfigManager<String> {

    public static final RebuildApiManager instance = new RebuildApiManager();
    private RebuildApiManager() {}

    /**
     * @param appid
     * @return
     */
    public ConfigEntry getApp(String appid) {
        final String cKey = "RebuildApiManager-" + appid;
        ConfigEntry config = (ConfigEntry) Application.getCommonCache().getx(cKey);
        if (config != null) {
            return config;
        }

         Object[] o = Application.createQueryNoFilter(
                "select appSecret,bindUser,bindIps from RebuildApi where appId = ?")
                 .setParameter(1, appid)
                 .unique();
        if (o == null) {
            return null;
        }

        config = new ConfigEntry()
                .set("appId", appid)
                .set("appSecret", o[0])
                .set("bindUser", o[1])
                .set("bindIps", o[2]);
        Application.getCommonCache().putx(cKey, config);
        return config;
    }

    @Override
    public void clean(String cacheKey) {
        final String cKey = "RebuildApiManager-" + cacheKey;
        Application.getCommonCache().evict(cKey);
    }
}
