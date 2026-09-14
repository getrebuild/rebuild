/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 修订列表模板公式
 *
 * @author Zixin
 * @since 2022/12/14
 */
@Slf4j
public class FormulaCellWriteHandler implements CellWriteHandler {

    private static final Pattern PATT_CELLNO = Pattern.compile("([A-Z]+[0-9]+)");

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        setCellFormula(context);
    }

    private void setCellFormula(CellWriteHandlerContext context) {
        final Cell cell = context.getCell();
        String cellValue;
        try {
            cellValue = cell.getStringCellValue();
        } catch (Exception e) {
            return;
        }
        // {.__KEEP:(=B2>1000)}
        if (StringUtils.isBlank(cellValue) || !(cellValue.startsWith("(=") && cellValue.endsWith(")"))) return;

        final int rowIndex = cell.getRowIndex() + 1;

        String cellFormula = cellValue.substring(2, cellValue.length() - 1);
        Matcher m = PATT_CELLNO.matcher(cellValue);
        Set<String> set = new HashSet<>();
        while (m.find()) {
            String cellNo = m.group(1);
            // v3.9.2 验证以防错误匹配 Gitee#IBI8UQ
            if (!cellNo.matches("[A-Z]{1,2}[0-9]{1,3}")) {
                log.warn("Bad cell no matches : {}", cellNo);
                continue;
            }
            // 避免多次替换出错
            if (set.contains(cellNo)) continue;
            set.add(cellNo);

            String cellNoNew = cellNo.replaceAll("[0-9]+", String.valueOf(rowIndex));
            cellFormula = cellFormula.replace(cellNo, cellNoNew);
        }

        try {
            cell.setCellFormula(cellFormula);
        } catch (NullPointerException e) {
            // POI 的 arrayFormulas 缓存在 shiftRows 后不更新，导致 setCellFormula/
            // removeArrayFormula/setBlank 均 NPE，通过反射清除过期缓存条目
            if (cell instanceof XSSFCell) {
                XSSFCell xssfCell = (XSSFCell) cell;
                try {
                    Field f = XSSFSheet.class.getDeclaredField("arrayFormulas");
                    f.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<CellRangeAddress> list = (List<CellRangeAddress>) f.get(xssfCell.getSheet());
                    if (list != null) {
                        int row = xssfCell.getRowIndex();
                        int col = xssfCell.getColumnIndex();
                        list.removeIf(r -> r.isInRange(row, col));
                    }
                } catch (Exception ex) {
                    log.warn("Failed to clear stale array formula cache", ex);
                }
            }
            cell.setCellFormula(cellFormula);
        }
    }
}
