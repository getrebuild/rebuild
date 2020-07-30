/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.configuration;

import com.rebuild.server.Application;

/**
 * API 鉴权参数
 *
 * @author ZHAO
 * @since 2019-07-23
 */
public class RebuildApiManager implements ConfigManager {

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
    public void clean(Object appid) {
        final String cKey = "RebuildApiManager-" + appid;
        Application.getCommonCache().evict(cKey);
    }
}
