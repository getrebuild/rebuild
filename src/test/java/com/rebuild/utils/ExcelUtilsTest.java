/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.excel.Cell;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.util.List;

/**
 * @author devezhao
 * @since 2020/7/15
 */
public class ExcelUtilsTest {

    @Test
    public void readExcel() throws Exception {
        List<Cell[]> rows = ExcelUtils.readExcel(
                ResourceUtils.getFile("classpath:dataimports-test.xlsx"));
        for (Cell[] row : rows) {
            System.out.println(StringUtils.join(row, " | "));
        }

        rows = ExcelUtils.readExcel(
                ResourceUtils.getFile("classpath:dataimports-test.xls"));
        for (Cell[] row : rows) {
            System.out.println(StringUtils.join(row, " | "));
        }
    }
}
