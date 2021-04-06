/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.merge.AbstractMergeStrategy;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;

import java.util.List;

/**
 * THANKS https://blog.csdn.net/weixin_44682948/article/details/112897500
 *
 * @author devezhao
 * @since 2021/4/3
 */
public class FixsMergeStrategy extends AbstractMergeStrategy {

    @Override
    protected void merge(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
        if (relativeRowIndex == null || relativeRowIndex == 0) {
            return;
        }

        int rowIndex = cell.getRowIndex();
        int colIndex = cell.getColumnIndex();
        sheet = cell.getSheet();
        Row prevRow = sheet.getRow(rowIndex - 1);
        Cell prevCell = prevRow.getCell(colIndex);
        List<CellRangeAddress> craList = sheet.getMergedRegions();
        CellStyle cs = cell.getCellStyle();
        cell.setCellStyle(cs);

        for (CellRangeAddress cellRangeAddress : craList) {
            if (cellRangeAddress.containsRow(prevCell.getRowIndex()) && cellRangeAddress.containsColumn(prevCell.getColumnIndex())) {
                int lastColIndex = cellRangeAddress.getLastColumn();
                int firstColIndex = cellRangeAddress.getFirstColumn();
                CellRangeAddress cra = new CellRangeAddress(cell.getRowIndex(), cell.getRowIndex(), firstColIndex, lastColIndex);
                sheet.addMergedRegion(cra);

                // 复制线框样式
                RegionUtil.setBorderBottom(cs.getBorderBottom(), cra, sheet);
                RegionUtil.setBorderLeft(cs.getBorderLeft(), cra, sheet);
                RegionUtil.setBorderRight(cs.getBorderRight(), cra, sheet);
                RegionUtil.setBorderTop(cs.getBorderTop(), cra, sheet);

                break;
            }
        }
    }
}
