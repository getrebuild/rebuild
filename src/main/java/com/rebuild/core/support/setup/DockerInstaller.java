/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.OshiUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;

/**
 * Docker 安装
 *
 * @author devezhao
 * @since 2025/4/10
 */
@Slf4j
public class DockerInstaller extends Installer {

    public DockerInstaller() {
        super(buildInstallProps());
    }

    // FAKE
    static JSONObject buildInstallProps() {
        JSONObject installProps = new JSONObject();
        JSONObject databaseProps = JSONUtils.toJSONObject(
                new String[]{"dbName", "dbHost", "dbPort", "dbUser", "dbPassword"},
                new String[]{"rebuild40", "mysql", "3306", "root", "rebuildP4wd"});
        if (Application.devMode()) {
            databaseProps.put("dbHost", "localhost");
            databaseProps.put("dbUser", "rebuild");
            databaseProps.put("dbPassword", "rebuild");
        }
        installProps.put("databaseProps", databaseProps);
        return installProps;
    }

    @Override
    public void install() throws Exception {
        this.installDatabase();
    }

    /**
     */
    public void installAfter() {
        try {
            this.installClassificationAsync();
        } catch (Exception ex) {
            log.error("Error installing classification data", ex);
        }
    }

    /**
     * @return
     */
    public boolean isNeedInitialize() {
        if (!OshiUtils.isDockerEnv()) return false;
        if (!BooleanUtils.toBoolean(System.getProperty("initialize"))) return false;

        if (Installer.isInstalled()) {
            return !isRbDatabase();
        }
        return false;
    }
}