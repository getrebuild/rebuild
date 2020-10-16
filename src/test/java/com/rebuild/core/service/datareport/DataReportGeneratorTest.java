/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author devezhao
 * @since 2019/8/16
 */
public class DataReportGeneratorTest extends TestSupport {

    @Test
    public void testGeneratorV2Simple() throws FileNotFoundException {
        File template = ResourceUtils.getFile("classpath:report-template-v2.xlsx");
        ID record = addRecordOfTestAllFields(SIMPLE_USER);

        File file = new EasyExcelGenerator(template, record).setUser(UserService.ADMIN_USER).generate();
        System.out.println("Report : " + file);
    }

    @Test
    public void testGeneratorV2() throws FileNotFoundException {
        UserContextHolder.setUser(UserService.ADMIN_USER);

        File template = ResourceUtils.getFile("classpath:report-template-v2.xlsx");

        Entity SalesOrder999 = MetadataHelper.getEntity(SalesOrder);
        Record record = EntityHelper.forNew(SalesOrder999.getEntityCode(), SIMPLE_USER);
        record = Application.getEntityService(SalesOrder999.getEntityCode()).create(record);

        // 主记录+明细记录
        File file = new EasyExcelGenerator(template, record.getPrimary()).setUser(UserService.ADMIN_USER).generate();
        System.out.println("Report : " + file);
    }
}
