/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
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

    @Override
    protected ID getSessionUser() {
        return UserService.ADMIN_USER;
    }

    @Test
    public void testGenerator() throws Exception {
        File template = ResourceUtils.getFile("classpath:report-template.xlsx");
        ID record = addRecordOfTestAllFields();
        ReportGenerator generator = new ReportGenerator(template, record);
        generator.setUser(UserService.ADMIN_USER);
        File file = generator.generate();
        System.out.println(file);
    }
}
