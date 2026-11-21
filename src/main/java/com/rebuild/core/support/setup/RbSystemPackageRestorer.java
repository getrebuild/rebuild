/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import cn.devezhao.commons.sql.SqlBuilder;
import cn.devezhao.commons.sql.builder.UpdateBuilder;
import com.rebuild.core.Application;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 已安装系统中恢复应用资源包（仅限全新安装且无业务实体时）
 *
 * @author RB
 * @see RbSystemPackageImporter
 * @since 2026/11/18
 */
@Slf4j
public class RbSystemPackageRestorer {

    private final RbSystemPackageImporter importer;

    /**
     * @param tempFileName
     */
    public RbSystemPackageRestorer(String tempFileName) {
        this.importer = new RbSystemPackageImporter(tempFileName);
    }

    /**
     * 整库恢复
     */
    public void restore() throws Exception {
        if (MetadataSorter.sortEntities(null, false, false).length > 0) {
            throw new RebuildException("当前系统已有业务数据，不能导入应用资源包");
        }

        // 先解压校验包结构，避免包损坏时库已被清空
        importer.unpack();

        // 保留安装相关的数据
        String adminPassword = null;
        String adminMail = null;
        Object[] admin = Application.createQueryNoFilter(
                "select password,email from User where userId = ?")
                .setParameter(1, UserService.ADMIN_USER)
                .unique();
        if (admin != null) {
            adminPassword = (String) admin[0];
            adminMail = (String) admin[1];
        }
        String SN = RebuildConfiguration.get(ConfigurationItem.SN, true);
        String installBuild = RebuildConfiguration.get(ConfigurationItem.InstallBuild, true);

        DataSource dataSource = Application.getBean(DataSource.class);
        try (Connection conn = dataSource.getConnection()) {
            dropAllTables(conn);
            importer.restore(conn);
        }

        // 刷新缓存
        MetadataHelper.getMetadataFactory().refresh();
        Application.getUserStore().refresh();
        Installer.clearAllCache();

        if (StringUtils.isNotBlank(adminPassword)) {
            UpdateBuilder ub = SqlBuilder.buildUpdate("user");
            ub.addColumn("PASSWORD", adminPassword);
            if (StringUtils.isNotBlank(adminMail)) ub.addColumn("EMAIL", adminMail);
            ub.setWhere(String.format("USER_ID = '%s'", UserService.ADMIN_USER));
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(ub.toSql());
            }
        }

        if (StringUtils.isNotBlank(SN)) RebuildConfiguration.set(ConfigurationItem.SN, SN);
        if (StringUtils.isNotBlank(installBuild)) RebuildConfiguration.set(ConfigurationItem.InstallBuild, installBuild);

        importer.cleanup();
        log.info("Restored rbsystem package in installed system");
    }

    /**
     * @param conn
     * @throws SQLException
     */
    private void dropAllTables(Connection conn) throws SQLException {
        final boolean h2 = Installer.isUseH2();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(h2 ? "SET REFERENTIAL_INTEGRITY FALSE" : "SET FOREIGN_KEY_CHECKS=0");
        }

        List<String> tables = new ArrayList<>();
        DatabaseMetaData dmd = conn.getMetaData();
        String schema = h2 ? conn.getSchema() : null;
        try (ResultSet rs = dmd.getTables(conn.getCatalog(), schema, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }

        try (Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                stmt.executeUpdate("DROP TABLE IF EXISTS `" + table + "`");
            }
            log.info("Dropped tables before restore : {}", tables.size());
        } finally {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(h2 ? "SET REFERENTIAL_INTEGRITY TRUE" : "SET FOREIGN_KEY_CHECKS=1");
            }
        }
    }
}
