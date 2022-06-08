/*!
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
public class SysbaseDiagnosis {

    private static final String CKEY_DANGERS = "_DANGERS";

    private static final String HasUpdate = "HasUpdate";
    private static final String AdminMsg = "AdminMsg";
    private static final String UsersMsg = "UsersMsg";
    private static final String CommercialNoRbv = "CommercialNoRbv";

    public static final String DatabaseBackupFail = "DatabaseBackupFail";
    public static final String DataFileBackupFail = "DataFileBackupFail";

    public static String _DENIEDMSG = null;

    public void diagnose() {
        ServerStatus.getLastStatus(true);

        LinkedHashMap<String, String> dangers = getDangersList();

        if (License.getCommercialType() != 11) {
            JSONObject checkBuild = License.siteApi("api/authority/check-build");
            if (checkBuild != null && checkBuild.getIntValue("build") > Application.BUILD) {
                dangers.put(HasUpdate, checkBuild.getString("version") + "$$$$" + checkBuild.getString("releaseUrl"));
            } else {
                dangers.remove(HasUpdate);
            }
        }

        JSONObject echoValidity = License.siteApiNoCache("api/authority/echo?once=" + ServerStatus.STARTUP_ONCE);
        if (echoValidity != null && !echoValidity.isEmpty()) {
            String adminMsg = echoValidity.getString("adminMsg");
            if (adminMsg == null) dangers.remove(AdminMsg);
            else dangers.put(AdminMsg, adminMsg);

            String usersMsg = echoValidity.getString("usersMsg");
            if (usersMsg == null) dangers.remove(UsersMsg);
            else dangers.put(UsersMsg, usersMsg);

            // MULTIPLE RUNNING INSTANCES DETECTED!
            _DENIEDMSG = echoValidity.getString("deniedMsg");

        } else {
            dangers.remove(AdminMsg);
            dangers.remove(UsersMsg);
        }

        Application.getCommonsCache().putx(CKEY_DANGERS, dangers, CommonsCache.TS_DAY);
    }

    @SuppressWarnings("unchecked")
    static LinkedHashMap<String, String> getDangersList() {
        LinkedHashMap<String, String> dangers = (LinkedHashMap<String, String>)
                Application.getCommonsCache().getx(CKEY_DANGERS);
        return dangers == null ? new LinkedHashMap<>() : (LinkedHashMap<String, String>) dangers.clone();
    }

    public static void setItem(String name, String message) {
        LinkedHashMap<String, String> dangers = getDangersList();
        if (message == null) dangers.remove(name);
        else dangers.put(name, message);

        Application.getCommonsCache().putx(CKEY_DANGERS, dangers, CommonsCache.TS_DAY * 2);
    }

    public static Collection<String> getAdminDanger() {
        LinkedHashMap<String, String> dangers = getDangersList();

        if (License.isCommercial() && !License.isRbvAttached()) {
            dangers.put(CommercialNoRbv,
                    Language.L("系统检测到增值功能包未安装，相关增值功能可能无法使用。请联系 REBUILD 服务人员获取"));
        }

        if (dangers.isEmpty()) return null;

        dangers.remove(UsersMsg);

        String hasUpdate = dangers.get(HasUpdate);
        if (hasUpdate != null && hasUpdate.contains("$$$$")) {
            String[] ss = hasUpdate.split("\\$\\$\\$\\$");
            hasUpdate = Language.L("有新版的 REBUILD (%s) 更新可用 [(查看详情)](%s)", ss[0], ss[1]);
            hasUpdate = hasUpdate.replace("<a ", "<a target=\"_blank\" ");
            dangers.put(HasUpdate, hasUpdate);
        }

        if (RebuildConfiguration.getBool(ConfigurationItem.DBBackupsEnable)) {
            String hasDatabaseBackupFail = dangers.get(DatabaseBackupFail);
            if (hasDatabaseBackupFail != null) {
                dangers.put(DatabaseBackupFail,
                        Language.L("数据备份失败") + String.format("<blockquote class=\"code\">%s</blockquote>", hasDatabaseBackupFail));
            }

            String hasDataFileBackupFail = dangers.get(DataFileBackupFail);
            if (hasDataFileBackupFail != null) {
                dangers.put(DataFileBackupFail,
                        Language.L("数据备份失败") + String.format("<blockquote class=\"code\">%s</blockquote>", hasDataFileBackupFail));
            }
        }

        return dangers.values();
    }

    public static String getUsersDanger() {
        LinkedHashMap<String, String> dangers = getDangersList();
        return dangers.get(UsersMsg);
    }
}
