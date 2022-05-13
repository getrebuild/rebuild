/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import com.rebuild.core.Application;

/**
 * API 鉴权参数
 *
 * @author ZHAO
 * @since 2019-07-23
 */
public class RebuildApiManager implements ConfigManager {

    public static final RebuildApiManager instance = new RebuildApiManager();

    private RebuildApiManager() {
    }

    private static final String CKEY_PREFIX = "RebuildApiManager-";

    /**
     * @param appid
     * @return
     */
    public ConfigBean getApp(String appid) {
        final String ckey = CKEY_PREFIX + appid;
        ConfigBean cb = (ConfigBean) Application.getCommonsCache().getx(ckey);
        if (cb != null) {
            return cb;
        }

        Object[] o = Application.createQueryNoFilter(
                "select appSecret,bindUser,bindIps from RebuildApi where appId = ?")
                .setParameter(1, appid)
                .unique();
        if (o == null) return null;

        cb = new ConfigBean()
                .set("appId", appid)
                .set("appSecret", o[0])
                .set("bindUser", o[1])
                .set("bindIps", o[2]);
        Application.getCommonsCache().putx(ckey, cb);
        return cb;
    }

    @Override
    public void clean(Object appid) {
        Application.getCommonsCache().evict(CKEY_PREFIX + appid);
    }
}
