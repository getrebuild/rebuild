/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import cn.devezhao.persist4j.util.SqlHelper;
import cn.hutool.core.util.ZipUtil;
import com.rebuild.core.RebuildException;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 应用资源包导入
 *
 * @author RB
 * @see Installer
 * @since 2026/10/30
 */
@Slf4j
public class RbSystemPackageImporter {

    public static final String[] ATTACHMENT_DIRS = new String[]{
            "REPORT_TEMPLATES", "PROXYTRIGGER_CLASSES", "AIBOT_KBS"
    };

    private final File packageFile;
    private File rbspkgDir;

    /**
     * @param tempFileName
     */
    public RbSystemPackageImporter(String tempFileName) {
        this(RebuildConfiguration.getFileOfTemp(tempFileName));
    }

    /**
     * @param packageFile
     */
    public RbSystemPackageImporter(File packageFile) {
        this.packageFile = packageFile;
    }

    /**
     * 完整恢复
     *
     * @param conn
     * @throws SQLException
     * @throws IOException
     */
    public void restore(Connection conn) throws SQLException, IOException {
        if (!packageFile.exists()) {
            throw new RebuildException("Package not found : " + packageFile);
        }

        File dest = RebuildConfiguration.getFileOfTemp("__RBSPKG_INSTALL");
        if (dest.exists()) FileUtils.forceDelete(dest);
        ZipUtil.unzip(packageFile, dest);
        rbspkgDir = new File(dest, "rbspkg");
        if (!rbspkgDir.exists()) {
            throw new RebuildException("Bad rbsystem package : rbspkg/ not found");
        }

        executeSqlFile(conn, new File(rbspkgDir, "schema.sql"));
        executeSqlFile(conn, new File(rbspkgDir, "data.sql"));

        for (String dir : ATTACHMENT_DIRS) {
            File src = new File(rbspkgDir, dir);
            if (!src.exists()) continue;

            File destDir = RebuildConfiguration.getFileOfData("rb/" + dir);
            if (!destDir.exists()) FileUtils.forceMkdir(destDir);
            FileUtils.copyDirectory(src, destDir);
            log.info("Restored attachment directory : {}", dir);
        }

        log.info("Database restored from rbsystem package");
    }

    /**
     * 清理解压目录
     */
    public void cleanup() {
        if (rbspkgDir != null) {
            FileUtils.deleteQuietly(rbspkgDir.getParentFile());
        }
    }

    private void executeSqlFile(Connection conn, File sqlFile) throws SQLException, IOException {
        Statement stmt = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(sqlFile))) {
            stmt = conn.createStatement();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("--") || line.startsWith("/*")) continue;
                sb.append(line);

                if (line.endsWith(";")) {
                    stmt.execute(sb.toString());
                    sb = new StringBuilder();
                } else {
                    sb.append("\n");
                }
            }
        } finally {
            SqlHelper.close(stmt);
        }
    }
}
