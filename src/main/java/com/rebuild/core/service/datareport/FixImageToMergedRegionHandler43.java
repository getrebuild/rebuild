/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import com.alibaba.excel.write.handler.WorkbookWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.ClientAnchor.AnchorType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.List;

/**
 * 修订合并单元格图片大小问题
 *
 * @author RB
 * @since 2026/1/13
 */
@Slf4j
public class FixImageToMergedRegionHandler43 implements WorkbookWriteHandler {

    @Override
    public void afterWorkbookDispose(WriteWorkbookHolder writeWorkbookHolder) {
        Workbook wb = writeWorkbookHolder.getWorkbook();
        try {
            // .xlsx
            if (wb instanceof XSSFWorkbook) {
                xssf((XSSFWorkbook) wb);
            } else if (wb instanceof SXSSFWorkbook) {
                xssf(((SXSSFWorkbook) wb).getXSSFWorkbook());
            }
            // .xls
            else if (wb instanceof HSSFWorkbook) {
                hssf((HSSFWorkbook) wb);
            }
        } catch (Exception ex) {
            log.error("Error on wb : {}", wb, ex);
        }
    }

    private void xssf(XSSFWorkbook xssfWb) {
        for (int s = 0; s < xssfWb.getNumberOfSheets(); s++) {
            XSSFSheet sheet = xssfWb.getSheetAt(s);

            // sheet.getDrawingPatriarch() 可能为空（有时需要 createDrawingPatriarch）
            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            if (drawing == null) continue;

            List<XSSFShape> shapes = drawing.getShapes();
            if (shapes.isEmpty()) continue;

            for (XSSFShape shape : shapes) {
                if (!(shape instanceof XSSFPicture)) continue;

                XSSFPicture pic = (XSSFPicture) shape;
                XSSFClientAnchor anchor = pic.getPreferredSize(); // 当前锚点（一般就是单元格锚）
                if (anchor == null) continue;

                int row1 = anchor.getRow1();
                int col1 = anchor.getCol1();

                // 找“图片左上角所在单元格”归属的合并区域
                CellRangeAddress merged = findMergedRegion(sheet, row1, col1);
                if (merged == null) continue;

                System.out.println(merged);

                // 把图片锚点改为覆盖整个合并区域
                anchor.setRow1(merged.getFirstRow());
                anchor.setCol1(merged.getFirstColumn());
                anchor.setRow2(merged.getLastRow() + 1);
                anchor.setCol2(merged.getLastColumn() + 1);

                // 让图片随单元格移动并缩放，才能真正“铺满并跟随”
                anchor.setAnchorType(AnchorType.MOVE_AND_RESIZE);

                // 清零偏移，让边缘贴合单元格边界（可选）
                anchor.setDx1(0);
                anchor.setDy1(0);
                anchor.setDx2(0);
                anchor.setDy2(0);

                // 把修改后的 anchor 写回（XSSFPicture 的 anchor 就是这个对象本身，通常改完即生效）
                // 不要调用 pic.resize()，否则会按图片比例重新计算范围，导致不铺满
            }

            // 第一个 sheet
            break;
        }
    }

    private void hssf(HSSFWorkbook hssfWb) {
        for (int s = 0; s < hssfWb.getNumberOfSheets(); s++) {
            HSSFSheet sheet = hssfWb.getSheetAt(s);

            HSSFPatriarch patriarch = sheet.getDrawingPatriarch();
            if (patriarch == null) continue;

            List<HSSFShape> children = patriarch.getChildren();
            if (children.isEmpty()) continue;

            for (HSSFShape shape : children) {
                if (!(shape instanceof HSSFPicture)) continue;

                HSSFPicture pic = (HSSFPicture) shape;
                HSSFClientAnchor anchor = (HSSFClientAnchor) pic.getAnchor();
                if (anchor == null) continue;

                int row1 = anchor.getRow1();
                int col1 = anchor.getCol1();

                CellRangeAddress merged = findMergedRegion(sheet, row1, col1);
                if (merged == null) continue;

                anchor.setRow1(merged.getFirstRow());
                anchor.setCol1(merged.getFirstColumn());
                anchor.setRow2(merged.getLastRow() + 1);
                anchor.setCol2(merged.getLastColumn() + 1);

                anchor.setAnchorType(AnchorType.MOVE_AND_RESIZE);

                // 贴边（可选）：HSSF 的 dx/dy 单位是 1/1024(列) 和 1/256(行) 的偏移
                anchor.setDx1(0);
                anchor.setDy1(0);
                anchor.setDx2(0);
                anchor.setDy2(0);

                // 同样不要调用 pic.resize()
            }

            // 第一个 sheet
            break;
        }
    }

    private CellRangeAddress findMergedRegion(Sheet sheet, int r, int c) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress a = sheet.getMergedRegion(i);
            if (a.isInRange(r, c)) {
//                if (a.getFirstRow() == a.getLastRow()) return null;
//                if (a.getFirstColumn() + 1 == a.getLastColumn()) return null;
                return a;
            }
        }
        return null;
    }
}
