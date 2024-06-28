/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.merge.AbstractMergeStrategy;
import com.esotericsoftware.minlog.Log;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.List;

/**
 * THANKS https://blog.csdn.net/weixin_44682948/article/details/112897500
 *
 * @author devezhao
 * @since 2021/4/3
 */
@Slf4j
public class FixsMergeStrategy extends AbstractMergeStrategy {

    @Override
    protected void merge(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
        if (relativeRowIndex == null || relativeRowIndex == 0) {
            return;
        }

//        int rowIndex = cell.getRowIndex();
//        int colIndex = cell.getColumnIndex();
//        sheet = cell.getSheet();
//        Row prevRow = sheet.getRow(rowIndex - 1);
//        if (prevRow == null) return;
//        Cell prevCell = prevRow.getCell(colIndex);
//        List<CellRangeAddress> craList = sheet.getMergedRegions();
//        CellStyle cs = cell.getCellStyle();
//        cell.setCellStyle(cs);
//
//        for (CellRangeAddress cellRangeAddress : craList) {
//            if (cellRangeAddress.containsRow(prevCell.getRowIndex()) && cellRangeAddress.containsColumn(prevCell.getColumnIndex())) {
//                int lastColIndex = cellRangeAddress.getLastColumn();
//                int firstColIndex = cellRangeAddress.getFirstColumn();
//                CellRangeAddress cra = new CellRangeAddress(cell.getRowIndex(), cell.getRowIndex(), firstColIndex, lastColIndex);
//                sheet.addMergedRegion(cra);
//
//                // 复制线框样式
//                RegionUtil.setBorderBottom(cs.getBorderBottom(), cra, sheet);
//                RegionUtil.setBorderLeft(cs.getBorderLeft(), cra, sheet);
//                RegionUtil.setBorderRight(cs.getBorderRight(), cra, sheet);
//                RegionUtil.setBorderTop(cs.getBorderTop(), cra, sheet);
//
//                break;
//            }
//        }

        try {
            merge37(sheet, cell, head, relativeRowIndex);
        } catch (Exception ex) {
            log.warn("Cannot merge cell", ex);
        }
    }

    // v3.7
    // THANKS https://github.com/alibaba/easyexcel/issues/2963#issuecomment-1432827475
    private void merge37(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
        int rowIndex = cell.getRowIndex();
        int colIndex = cell.getColumnIndex();
        Sheet thisSheet = cell.getSheet();
        Row preRow = thisSheet.getRow(rowIndex - 1);
        Row thisRow = thisSheet.getRow(rowIndex);
        Cell preCell = preRow.getCell(colIndex);
        Cell tmpCell;
        List<CellRangeAddress> list = thisSheet.getMergedRegions();

        for (int i = 0; i < list.size(); i++) {
            CellRangeAddress cellRangeAddress = list.get(i);
            if (cellRangeAddress.containsRow(preCell.getRowIndex()) && cellRangeAddress.containsColumn(preCell.getColumnIndex())) {
                int lastColIndex = cellRangeAddress.getLastColumn();
                int firstColIndex = cellRangeAddress.getFirstColumn();
                CellRangeAddress cra = new CellRangeAddress(cell.getRowIndex(), cell.getRowIndex(), firstColIndex, lastColIndex);
                thisSheet.addMergedRegion(cra);
                for (int j = firstColIndex; j <= lastColIndex; j++) {
                    tmpCell = thisRow.getCell(j);
                    if (tmpCell == null) {
                        tmpCell = thisRow.createCell(j);
                    }
                    tmpCell.setCellStyle(preRow.getCell(j).getCellStyle());
                }
                return;
            }
        }
    }
}
