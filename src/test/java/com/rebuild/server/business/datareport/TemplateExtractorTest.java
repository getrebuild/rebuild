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

import cn.devezhao.persist4j.Entity;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.Set;

/**
 * @author devezhao
 * @since 2019/8/17
 */
public class TemplateExtractorTest extends TestSupport {

    @Test
    public void testExtractVars() throws Exception {
        File template = ResourceUtils.getFile("classpath:report-template.xlsx");
        Set<String> vars = new TemplateExtractor(template).extractVars(true);
        System.out.println(vars);
        Assert.assertTrue(vars.size() >= 7);
    }

    @Test
    public void testGetRealField() throws Exception {
        Entity test = MetadataHelper.getEntity(TEST_ENTITY);

        String field = new TemplateExtractor(null).getRealField(test, "所属用户");
        System.out.println(field);

        String field2 = new TemplateExtractor(null).getRealField(test, "所属用户.姓名");
        System.out.println(field2);

        String field3 = new TemplateExtractor(null).getRealField(test, "所属用户.不存在的字段");
        System.out.println(field3);
    }
}
