/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 列表模板公式
 *
 * @author Zixin
 * @since 2022/12/14
 */
public class FormulaCellWriteHandler implements CellWriteHandler {

    private static final Pattern PATT_CELLNO = Pattern.compile("([A-Z]+[0-9]+)");

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        setCellFormula(context);
    }

    private void setCellFormula(CellWriteHandlerContext context) {
        final Cell cell = context.getCell();
        if (cell.getCellType() != CellType.STRING) return;
        final String cellValue = cell.getStringCellValue();
        // {.__KEEP:(=B2>1000)}
        if (StringUtils.isBlank(cellValue) || !(cellValue.startsWith("(=") && cellValue.endsWith(")"))) return;

        final int rowIndex = cell.getRowIndex() + 1;

        String cellFormula = cellValue.substring(2, cellValue.length() - 1);
        Matcher m = PATT_CELLNO.matcher(cellValue);
        while (m.find()) {
            String cellNo = m.group(1);
            String cellNoNew = cellNo.replaceAll("[0-9]+", rowIndex + "");
            cellFormula = cellFormula.replace(cellNo, cellNoNew);
        }

        cell.setCellFormula(cellFormula);
    }
}
