/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.ServerStatus;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.support.i18n.Language;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * @author devezhao
 * @since 2020/12/7
 */
@SuppressWarnings("unchecked")
public class CheckDangers {

    private static final String CKEY_DANGERS = "_DANGERS";

    /**
     */
    public void checks() {
        // Status
        ServerStatus.getLastStatus(true);

        LinkedHashMap<String, String> dangers = (LinkedHashMap<String, String>) Application.getCommonsCache().getx(CKEY_DANGERS);
        if (dangers == null) {
            dangers = new LinkedHashMap<>();
        }

        JSONObject checkBuild = License.siteApi("api/authority/check-build", true);
        if (checkBuild != null && checkBuild.getIntValue("build") > Application.BUILD) {
            String hasUpdate = Language.LF(
                    "NewVersion", checkBuild.getString("version"), checkBuild.getString("releaseUrl"));
            hasUpdate = hasUpdate.replace("<a ", "<a target='_blank' class='link' ");
            dangers.put("HasUpdate", hasUpdate);
        } else {
            dangers.remove("HasUpdate");
        }

        JSONObject echoValidity = License.siteApi("api/authority/echo?once=" + ServerStatus.STARTUP_ONCE, false);
        if (echoValidity != null && !echoValidity.isEmpty()) {
            String adminMsg = echoValidity.getString("adminMsg");
            if (adminMsg == null) dangers.remove("AdminMsg");
            else dangers.put("AdminMsg", adminMsg);

            String usersMsg = echoValidity.getString("usersMsg");
            if (usersMsg == null) dangers.remove("UsersMsg");
            else dangers.put("UsersMsg", usersMsg);

        } else {
            dangers.remove("AdminMsg");
            dangers.remove("UsersMsg");
        }

        // 放入缓存
        Application.getCommonsCache().putx(CKEY_DANGERS, dangers, CommonsCache.TS_DAY * 2);
    }

    // --

    /**
     * @return
     */
    public static Collection<String> getAdminDangers() {
        LinkedHashMap<String, String> dangers = (LinkedHashMap<String, String>) Application.getCommonsCache().getx(CKEY_DANGERS);
        if (dangers == null || dangers.isEmpty()) {
            return null;
        }

        dangers = (LinkedHashMap<String, String>) dangers.clone();
        dangers.remove("UsersMsg");
        return dangers.values();
    }

    /**
     * @return
     */
    public static String getUserDanger() {
        LinkedHashMap<String, String> dangers = (LinkedHashMap<String, String>) Application.getCommonsCache().getx(CKEY_DANGERS);
        if (dangers == null || dangers.isEmpty()) {
            return null;
        }
        return dangers.get("UsersMsg");
    }
}
