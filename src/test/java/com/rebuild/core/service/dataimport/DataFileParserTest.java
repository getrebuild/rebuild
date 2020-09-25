/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

import cn.devezhao.commons.excel.Cell;
import com.rebuild.TestSupport;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
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

    static DataFileParser getDataFileParser(String fileName) throws FileNotFoundException {
        return new DataFileParser(ResourceUtils.getFile("classpath:" + fileName));
    }
}
