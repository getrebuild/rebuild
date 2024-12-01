/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.util.SqlHelper;
import cn.hutool.core.util.ZipUtil;
import com.rebuild.core.Application;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.impl.DynamicMetadataFactory;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.setup.Installer;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.utils.OkHttpUtils;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 系统导入
 *
 * @author devezhao
 * @see com.rebuild.core.support.setup.Installer
 * @since 2024/11/29
 */
public class RbSystemImporter extends HeavyTask<Integer> {

    private final String fileUrl;
    private File rbspkgDir;

    public RbSystemImporter(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    protected RbSystemImporter(File rbspkgDir) {
        this.fileUrl = null;
        this.rbspkgDir = rbspkgDir;
    }

    @Override
    protected Integer exec() throws Exception {
        // #1 准备文件
        this.readyFiles();

        // #2 保留必要参数
        final String holdSN = RebuildConfiguration.get(ConfigurationItem.SN);
        final String holdAppName = RebuildConfiguration.get(ConfigurationItem.AppName);
        final String holdLOGO = RebuildConfiguration.get(ConfigurationItem.LOGO);
        final String holdLOGOWhite = RebuildConfiguration.get(ConfigurationItem.LOGOWhite);
        final String holdHomeURL = RebuildConfiguration.get(ConfigurationItem.HomeURL);
        // 2.1# _READY=false
        final Field field_READY = Application.class.getDeclaredField("_READY");
        field_READY.setAccessible(true);
        field_READY.set(null, false);

        // TODO 保留用戶/部門/角色

        // #3 清空数据库
        this.forceEmptyDb();
        // #3.1 初始化数据库
        this.importDb();

        // #4 清空缓存
        Installer.clearAllCache();

        // #5 重载配置
        RebuildConfiguration.set(ConfigurationItem.SN, holdSN);
        RebuildConfiguration.set(ConfigurationItem.AppName, holdAppName);
        RebuildConfiguration.set(ConfigurationItem.LOGO, holdLOGO);
        RebuildConfiguration.set(ConfigurationItem.LOGOWhite, holdLOGOWhite);
        RebuildConfiguration.set(ConfigurationItem.HomeURL, holdHomeURL);
        // 刷新配置缓存
        for (ConfigurationItem item : ConfigurationItem.values()) {
            RebuildConfiguration.get(item, true);
        }
        // 加载自定义实体
        ((DynamicMetadataFactory) Application.getBean(PersistManagerFactory.class).getMetadataFactory()).refresh();
        // 字段还原
        field_READY.set(null, true);
        field_READY.setAccessible(false);

        // #6 报表模版
        File REPORT_TEMPLATES = new File(rbspkgDir, "REPORT_TEMPLATES");
        if (REPORT_TEMPLATES.exists()) {
            File dest = RebuildConfiguration.getFileOfData("rb/REPORT_TEMPLATES");
            if (!dest.exists()) FileUtils.forceMkdir(dest);
            FileUtils.copyDirectory(REPORT_TEMPLATES, dest);
        }

        return 1;
    }

    /**
     * 檢查
     *
     * @throws RebuildException
     */
    public void check() throws RebuildException {
        try {
            this.readyFiles();
        } catch (IOException e) {
            throw new RebuildException("CANNOT FETCH RBSPKG", e);
        }

        if (!new File(this.rbspkgDir, "rebuild.sql").exists()) {
            throw new RebuildException("BAD RBSPKG RESOURCES");
        }
    }

    private void readyFiles() throws IOException {
        if (rbspkgDir != null) return;

        File rbspkgDir = RebuildConfiguration.getFileOfTemp("__RBSPKG");
        if (rbspkgDir.exists()) FileUtils.forceDelete(rbspkgDir);
        FileUtils.forceMkdir(rbspkgDir);

        File rbspkg = OkHttpUtils.readBinary(RBStore.DATA_REPO + fileUrl);
        if (rbspkg == null) throw new RebuildException("Cannot fetch respkg : " + RBStore.DATA_REPO + fileUrl);

        ZipUtil.unzip(rbspkg, rbspkgDir);
        this.rbspkgDir = rbspkgDir;
    }

    private void forceEmptyDb() {
        Connection conn = Application.getSqlExecutor().getConnection();
        Statement stmt = null;
        Statement stmt2 = null;
        ResultSet rs = null;

        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SHOW TABLES");
            stmt2 = conn.createStatement();
            while (rs.next()) {
                String tableName = rs.getString(1);
                stmt2.executeUpdate("DROP TABLE IF EXISTS " + tableName);
            }

        } catch (SQLException ex) {
            throw new RebuildException(ex);
        } finally {
            SqlHelper.close(rs);
            SqlHelper.close(stmt);
            SqlHelper.close(stmt2);
            Application.getSqlExecutor().closeConnection(conn);
        }
    }

    private void importDb() {
        File fileOfDb = new File(this.rbspkgDir, "rebuild.sql");
        Connection conn = Application.getSqlExecutor().getConnection();
        Statement stmt = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(fileOfDb))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("--") || line.startsWith("/*")) continue;
                sb.append(line);

                if (line.endsWith(";")) {
                    if (stmt == null) stmt = conn.createStatement();
                    stmt.execute(sb.toString());
                    sb = new StringBuilder();
                } else {
                    sb.append("\n");
                }
            }

        } catch (Exception ex) {
            throw new RebuildException(ex);
        } finally {
            SqlHelper.close(stmt);
            Application.getSqlExecutor().closeConnection(conn);
        }
    }
}
