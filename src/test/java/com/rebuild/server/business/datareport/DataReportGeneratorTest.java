/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.datareport;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;

/**
 * @author devezhao
 * @since 2019/8/16
 */
public class DataReportGeneratorTest extends TestSupportWithUser {

    @SuppressWarnings("deprecation")
    @Test
    public void testGenerator() throws Exception {
        // 不支持中文变量
        File template = ResourceUtils.getFile("classpath:report-template.xlsx");
        ID record = addRecordOfTestAllFields();
        ReportGenerator generator = new ReportGenerator(template, record);
        generator.setUser(UserService.ADMIN_USER);
        File file = generator.generate();
        System.out.println(file);
    }

    @Test
    public void testGeneratorV2Simple() throws Exception {
        File template = ResourceUtils.getFile("classpath:report-template-v2.xlsx");
        ID record = addRecordOfTestAllFields();

        File file = new EasyExcelGenerator(template, record).setUser(UserService.ADMIN_USER).generate();
        System.out.println("Report : " + file);
    }

    @Test
    public void testGeneratorV2() throws Exception {
        File template = ResourceUtils.getFile("classpath:report-template-v2.xlsx");
        if (!MetadataHelper.containsEntity("SalesOrder999")) {
            return;
        }

        Entity SalesOrder999 = MetadataHelper.getEntity("SalesOrder999");
        Record record = EntityHelper.forNew(SalesOrder999.getEntityCode(), getSessionUser());
        record = Application.getEntityService(SalesOrder999.getEntityCode()).create(record);

        // 主记录+明细记录
        File file = new EasyExcelGenerator(template, record.getPrimary()).setUser(UserService.ADMIN_USER).generate();
        System.out.println("Report : " + file);
    }
}
