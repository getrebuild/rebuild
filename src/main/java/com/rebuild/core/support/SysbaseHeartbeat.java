/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.FileAppender;
import cn.devezhao.commons.CalendarUtils;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.ServerStatus;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.OshiUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * @author devezhao
 * @since 2020/12/7
 */
public class SysbaseHeartbeat {

    private static final String CKEY_DANGERS = "_DANGERS";

    private static final String HasUpdate = "HasUpdate";
    private static final String AdminMsg = "AdminMsg";
    private static final String UsersMsg = "UsersMsg";
    private static final String CommercialNoRbv = "CommercialNoRbv";
    private static final String DateNotSync = "DateNotSync";

    public static final String DatabaseBackupFail = "DatabaseBackupFail";
    public static final String DataFileBackupFail = "DataFileBackupFail";

    /**
     * Check server
     */
    public void heartbeat() {
        ServerStatus.getLastStatus(true);

        LinkedHashMap<String, String> dangers = getDangersList();

        // #1
        JSONObject checkBuild = License.siteApi("api/authority/check-build");
        if (checkBuild != null && checkBuild.getIntValue("build") > Application.BUILD) {
            dangers.put(HasUpdate,
                    checkBuild.getString("version") + CommonsUtils.COMM_SPLITER + checkBuild.getString("releaseUrl"));
        } else {
            dangers.remove(HasUpdate);
        }

        // #2
        JSONObject echoValidity = License.siteApiNoCache("api/authority/echo?once=" + ServerStatus.STARTUP_ONCE);
        if (echoValidity != null && !echoValidity.isEmpty()) {
            String adminMsg = echoValidity.getString("adminMsg");
            if (adminMsg == null) dangers.remove(AdminMsg);
            else dangers.put(AdminMsg, adminMsg);

            String usersMsg = echoValidity.getString("usersMsg");
            if (usersMsg == null) dangers.remove(UsersMsg);
            else dangers.put(UsersMsg, usersMsg);

        } else {
            dangers.remove(UsersMsg);
        }

        // #3
        Date networkDate = OshiUtils.getNetworkDate();
        long networkDateLeft = (networkDate.getTime() - CalendarUtils.now().getTime()) / 1000;
        if (Math.abs(networkDateLeft) > 15) {
            dangers.put(DateNotSync, String.valueOf(networkDateLeft));
        } else {
            dangers.remove(DateNotSync);
        }

        Application.getCommonsCache().putx(CKEY_DANGERS, dangers, CommonsCache.TS_DAY);
    }

    @SuppressWarnings("unchecked")
    static LinkedHashMap<String, String> getDangersList() {
        LinkedHashMap<String, String> dangers = (LinkedHashMap<String, String>)
                Application.getCommonsCache().getx(CKEY_DANGERS);
        return dangers == null ? new LinkedHashMap<>() : (LinkedHashMap<String, String>) dangers.clone();
    }

    /**
     * @param name
     * @param message
     */
    public static void setItem(String name, String message) {
        LinkedHashMap<String, String> dangers = getDangersList();
        if (message == null) dangers.remove(name);
        else dangers.put(name, message);

        Application.getCommonsCache().putx(CKEY_DANGERS, dangers, CommonsCache.TS_DAY * 2);
    }

    /**
     * @return
     */
    public static Collection<String> getAdminDanger() {
        LinkedHashMap<String, String> dangers = getDangersList();

        if (License.isCommercial() && !License.isRbvAttached()) {
            dangers.put(CommercialNoRbv,
                    Language.L("系统检测到增值功能包未安装，相关增值功能可能无法使用。请联系 REBUILD 服务人员获取"));
        }

        if (dangers.isEmpty()) return null;

        dangers.remove(UsersMsg);

        String hasUpdate = dangers.get(HasUpdate);
        if (hasUpdate != null && hasUpdate.contains(CommonsUtils.COMM_SPLITER)) {
            String[] ss = hasUpdate.split(CommonsUtils.COMM_SPLITER_RE);
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

        String hasNetworkDateLeft = dangers.get(DateNotSync);
        if (hasNetworkDateLeft != null) {
            dangers.put(DateNotSync,
                    Language.L("服务器时间与网络时间存在偏移，可能导致某些功能异常，建议检查并同步服务器时间"));
        }

        return dangers.values();
    }

    /**
     * @return
     */
    public static String getUsersDanger() {
        LinkedHashMap<String, String> dangers = getDangersList();
        return dangers.get(UsersMsg);
    }

    /**
     * @return
     */
    public static File getLogbackFile() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger lg = lc.getLogger("ROOT");
        FileAppender<?> fa = (FileAppender<?>) lg.getAppender("FILE");
        return new File(fa.getFile());
    }
}
