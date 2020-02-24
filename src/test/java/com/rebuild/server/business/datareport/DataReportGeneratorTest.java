/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.datareport;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.TestSupportWithUser;
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
        File template = ResourceUtils.getFile("classpath:report-template.xlsx");
        ID record = addRecordOfTestAllFields();
        ReportGenerator generator = new ReportGenerator(template, record);
        generator.setUser(UserService.ADMIN_USER);
        File file = generator.generate();
        System.out.println(file);
    }

    @Test
    public void testGeneratorV2() throws Exception {
        File template = ResourceUtils.getFile("classpath:report-template-v2.xlsx");
        ID record = ID.valueOf("997-017048e97a7900e1");  // SalesOrder999

        EasyExcelGenerator generator = new EasyExcelGenerator(template, record);
        try {
            File file = generator.setUser(UserService.ADMIN_USER).generate();
            System.out.println("Report : " + file);
        } catch (IllegalArgumentException ignored) {
            // No record found in CI env
        }
    }
}
