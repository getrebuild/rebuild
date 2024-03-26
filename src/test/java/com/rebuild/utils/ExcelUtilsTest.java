/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.excel.IRow;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 */
class ExcelUtilsTest {

    @Test
    void readExcelRows() throws IOException {
        IRow[] rows = ExcelUtils.readExcelRows(
                ResourceUtils.getFile("classpath:users-template.xls"));

        for (IRow row : rows) {
            System.out.println(row.getCell("A"));
            System.out.println(row.getCell("b"));
            System.out.println(row.getCell("G"));
            System.out.println(row.getCell("h"));
        }
    }

    @Test
    void saveToCsv() throws FileNotFoundException {
        File file = ResourceUtils.getFile("classpath:dataimports-test.xls");
        System.out.println(ExcelUtils.saveToCsv(file.toPath(), null));
    }
}