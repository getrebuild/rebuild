/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import cn.devezhao.persist4j.util.SqlHelper;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.BootEnvironmentPostProcessor;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.OshiUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Docker 安装
 *
 * @author devezhao
 * @since 2025/4/10
 */
@Slf4j
public class DockerInstaller extends Installer {

    public DockerInstaller() {
        super(new JSONObject());
    }

    @Override
    public void install() throws Exception {
        installProps = new JSONObject();
        JSONObject databaseProps = JSONUtils.toJSONObject(
                new String[]{"dbName", "dbHost", "dbPort", "dbUser", "dbPassword"},
                new String[]{"rebuild40", "localhost", "3306", "rebuild", "rebuild"});
        installProps.put("databaseProps", databaseProps);

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
        if (!BooleanUtils.toBoolean(System.getProperty("initialize"))) return false;

        if (Installer.isInstalled()) {
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(
                        BootEnvironmentPostProcessor.getProperty("db.url"),
                        BootEnvironmentPostProcessor.getProperty("db.user"),
                        BootEnvironmentPostProcessor.getProperty("db.passwd"));
                try (Statement stmt = conn.createStatement()) {
                    try (ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
                        // 非空则无需
                        return !rs.next();
                    }
                }

            } catch (Exception ex) {
                log.warn("Error check initialize state", ex);
            } finally {
                //noinspection deprecation
                SqlHelper.close(conn);
            }
        }

        // 必须有 `.rebuild` 文件
        return false;
    }
}