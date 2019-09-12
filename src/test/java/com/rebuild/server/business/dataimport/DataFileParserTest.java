/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.dataimport;

import cn.devezhao.commons.excel.Cell;
import com.rebuild.server.TestSupport;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * @author devezhao
 * @since 01/09/2019
 */
public class DataFileParserTest extends TestSupport {

    @Test
    public void testExcel() throws Exception {
        DataFileParser fileParser = getDataFileParser("dataimports-test.xls");

        System.out.println(fileParser.getRowsCount());
        List<Cell[]> rows = fileParser.parse(10);
        for (Cell[] r : rows) {
            System.out.println(StringUtils.join(r, " | "));
        }
    }

    @Test
    public void testCSV() throws Exception {
        DataFileParser fileParser = getDataFileParser("dataimports-test.csv");

        System.out.println(fileParser.getRowsCount());
        List<Cell[]> rows = fileParser.parse(10);
        for (Cell[] r : rows) {
            System.out.println(StringUtils.join(r, " | "));
        }
    }

    @Test
    public void testXExcel() throws Exception {
        DataFileParser fileParser = getDataFileParser("dataimports-test.xlsx");

        System.out.println(fileParser.getRowsCount());
        List<Cell[]> rows = fileParser.parse(50);
        for (Cell[] r : rows) {
            System.out.println(StringUtils.join(r, " | "));
        }
    }

    static DataFileParser getDataFileParser(String fileName) throws Exception {
        URL testFile = ResourceUtils.getURL("classpath:" + fileName);
        return new DataFileParser(new File(testFile.toURI()));
    }
}
