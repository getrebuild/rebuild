/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.OshiUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;

import static com.rebuild.core.support.ConfigurationItem.CacheHost;

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

        Properties installProps = buildConnectionProps(null);
        installProps.put(CONF_PREFIX + CacheHost.name(), "0");

        File dest = RebuildConfiguration.getFileOfData(INSTALL_FILE);
        try (OutputStream os = Files.newOutputStream(dest.toPath())) {
            installProps.store(os, "REBUILD INSTALLER MAGIC FOR DOCKER !!! DO NOT EDIT !!!");
            log.info("Saved installation file : {}", dest);
        }
    }

    /**
     * 安装后执行
     */
    public void installAfter() {
        try {
            this.installClassificationAsync();
        } catch (Exception ex) {
            log.error("Error init Classification", ex);
        }
    }

    /**
     * 是否需要安装
     *
     * @return
     */
    public boolean isNeedInitialize() {
        if (OshiUtils.isDockerEnv()
                && "docker".equals(System.getProperty("initialize"))) {
            return !Installer.isInstalled();
        }
        return false;
    }
}