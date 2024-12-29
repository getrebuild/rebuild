/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.exception.jdbc.ConstraintViolationException;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.BootApplication;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.DynamicMetadataContextHolder;
import com.rebuild.core.metadata.impl.Entity2Schema;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.rbstore.MetaschemaImporter;
import com.rebuild.core.support.setup.SimpleEntity;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.AppUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

/**
 * JUnit4 测试基类
 */
public class TestSupport {

    protected static final Logger _log = LoggerFactory.getLogger(TestSupport.class);

    private static boolean RebuildReady = false;

    @BeforeAll
    public static void setUp() {
        if (RebuildReady) return;
        _log.warn("TESTING Setup ...");

        try {
            System.setProperty("rbdev", "true");  // dev/debug mode
            System.setProperty("spring.main.web-application-type", "none");  // No Web
            System.setProperty("server.port", "0");  // random port
            BootApplication.main(new String[0]);
            RebuildReady = true;

            DynamicMetadataContextHolder.setSkipLanguageRefresh();
            if (addTestEntities(false)) {
                DynamicMetadataContextHolder.isSkipLanguageRefresh(true);
            }

        } catch (Exception ex) {
            _log.error("TESTING Setup failed!", ex);
            System.exit(-1);
        }
    }

    @AfterAll
    public static void setDown() {
        _log.warn("TESTING Setdown ...");

        UserContextHolder.clear();
    }

    @AfterEach
    public void setDownPerMethod() {
        UserContextHolder.clear();
    }

    // -- 测试实体

    // 全部字段类型
    protected static final String TestAllFields = SimpleEntity.NAME;

    // 业务实体
    protected static final String Account = "Account999";

    // 主-明细实体
    protected static final String SalesOrder = "SalesOrder999";
    protected static final String SalesOrderItem = "SalesOrderItem999";

    // -- 测试用户

    // 示例用户
    protected static final ID SIMPLE_USER = ID.valueOf("001-9000000000000001");
    // 示例部门
    protected static final ID SIMPLE_DEPT = ID.valueOf("002-9000000000000001");
    // 示例角色（无任何权限）
    protected static final ID SIMPLE_ROLE = ID.valueOf("003-9000000000000001");
    // 示例团队
    protected static final ID SIMPLE_TEAM = ID.valueOf("006-9000000000000001");

    /**
     * 添加测试用实体
     *
     * @param dropExists
     * @return
     * @throws Exception
     */
    @SuppressWarnings("SameParameterValue")
    protected static boolean addTestEntities(boolean dropExists) throws Exception {
        boolean changed = false;
        if (dropExists) {
            if (MetadataHelper.containsEntity(SalesOrder)) {
                _log.warn("Dropping test entity : " + SalesOrder);
                new Entity2Schema(UserService.ADMIN_USER).dropEntity(MetadataHelper.getEntity(SalesOrder), true);
            }
            if (MetadataHelper.containsEntity(Account)) {
                _log.warn("Dropping test entity : " + Account);
                new Entity2Schema(UserService.ADMIN_USER).dropEntity(MetadataHelper.getEntity(Account), true);
            }
        }

        new SimpleEntity().create(true, true, true);

        if (!MetadataHelper.containsEntity(Account)) {
            String metaschema = FileUtils.readFileToString(
                    ResourceUtils.getFile("classpath:schema-Account999.json"), AppUtils.UTF8);
            MetaschemaImporter importer = new MetaschemaImporter(JSON.parseObject(metaschema));
            TaskExecutors.run((HeavyTask<?>) importer.setUser(UserService.ADMIN_USER));
            changed = true;
        }
        if (!MetadataHelper.containsEntity(SalesOrder)) {
            String metaschema = FileUtils.readFileToString(
                    ResourceUtils.getFile("classpath:schema-SalesOrder999.json"), AppUtils.UTF8);
            MetaschemaImporter importer = new MetaschemaImporter(JSON.parseObject(metaschema));
            TaskExecutors.run((HeavyTask<?>) importer.setUser(UserService.ADMIN_USER));
            changed = true;
        }

        return changed;
    }

    /**
     * 添加一条测试记录
     *
     * @param user
     * @return
     */
    protected static ID addRecordOfTestAllFields(ID user) {
        if (user != null && UserContextHolder.getUser(true) == null) {
            UserContextHolder.setUser(user);
        }

        Entity testEntity = MetadataHelper.getEntity(TestAllFields);
        Record record = EntityHelper.forNew(testEntity.getEntityCode(), user);
        record.setString("TestAllFieldsName", "TestAllFieldsName-" + RandomUtils.nextLong());
        record.setString("text", "TEXT-" + RandomUtils.nextLong());

        // v3.7 自动修复自动编号重复问题
        while (true) {
            record.removeValue("SERIES");
            try {
                Application.getGeneralEntityService().create(record);
                break;
            } catch (ConstraintViolationException ex) {
                _log.warn("ConstraintViolationException : {}", ex.getLocalizedMessage());
            }
        }
        return record.getPrimary();
    }
}
