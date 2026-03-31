/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import com.alibaba.excel.write.handler.WorkbookWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.ClientAnchor;
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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 修订合并单元格图片大小问题
 *
 * @author RB
 * @since 2026/1/13
 */
@Slf4j
public class FixImageToMergedRegionHandler43 implements WorkbookWriteHandler {

    private List<String> originPics = null;

    protected FixImageToMergedRegionHandler43(File templateFile) {
        super();
        this.keepOriginPic(templateFile);
    }

    @Override
    public void afterWorkbookDispose(WriteWorkbookHolder writeWorkbookHolder) {
        Workbook wb = writeWorkbookHolder.getWorkbook();
        try {
            if (wb instanceof XSSFWorkbook) {
                xssf((XSSFWorkbook) wb, false);
            } else if (wb instanceof SXSSFWorkbook) {
                xssf(((SXSSFWorkbook) wb).getXSSFWorkbook(), false);
            } else if (wb instanceof HSSFWorkbook) {
                hssf((HSSFWorkbook) wb, false);
            }

        } catch (Exception ex) {
            log.error("Error fix image : {}", wb, ex);
        }
    }

    private void keepOriginPic(File templateFile) {
        if (templateFile == null) return;
        this.originPics = new ArrayList<>();

        if (templateFile.getName().toLowerCase().endsWith(".xlsx")) {
            try (FileInputStream is = new FileInputStream(templateFile);
                 XSSFWorkbook wb = new XSSFWorkbook(is)) {
                xssf(wb, true);
            } catch (Exception e) {
                log.error("Error on xlsx : {}", templateFile, e);
            }
        } else {
            try (FileInputStream is = new FileInputStream(templateFile);
                 HSSFWorkbook wb = new HSSFWorkbook(is)) {
                hssf(wb, true);
            } catch (Exception e) {
                log.error("Error on xls : {}", templateFile, e);
            }
        }
    }

    private void xssf(XSSFWorkbook xssfWb, boolean isGetPic) {
        for (int s = 0; s < xssfWb.getNumberOfSheets(); s++) {
            XSSFSheet sheet = xssfWb.getSheetAt(s);
            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            if (drawing == null) continue;

            List<XSSFShape> shapes = drawing.getShapes();
            if (shapes.isEmpty()) continue;

            for (XSSFShape shape : shapes) {
                if (!(shape instanceof XSSFPicture)) continue;

                XSSFPicture pic = (XSSFPicture) shape;
                if (isGetPic && originPics != null) {
                    originPics.add(DigestUtils.md5Hex(pic.getPictureData().getData()));
                    continue;
                } else if (CollectionUtils.isNotEmpty(originPics)) {
                    // 模版中已有的图片不处理
                    if (originPics.contains(DigestUtils.md5Hex(pic.getPictureData().getData()))) {
                        continue;
                    }
                }

                XSSFClientAnchor anchor = pic.getPreferredSize();
                if (anchor != null) this.imageToMerged(sheet, anchor);
            }

            // 仅第一个 sheet
            break;
        }
    }

    private void hssf(HSSFWorkbook hssfWb, boolean isGetPic) {
        for (int s = 0; s < hssfWb.getNumberOfSheets(); s++) {
            HSSFSheet sheet = hssfWb.getSheetAt(s);
            HSSFPatriarch patriarch = sheet.getDrawingPatriarch();
            if (patriarch == null) continue;

            List<HSSFShape> children = patriarch.getChildren();
            if (children.isEmpty()) continue;

            for (HSSFShape shape : children) {
                if (!(shape instanceof HSSFPicture)) continue;

                HSSFPicture pic = (HSSFPicture) shape;
                if (isGetPic && originPics != null) {
                    originPics.add(DigestUtils.md5Hex(pic.getPictureData().getData()));
                    continue;
                } else if (CollectionUtils.isNotEmpty(originPics)) {
                    // 模版中已有的图片不处理
                    if (originPics.contains(DigestUtils.md5Hex(pic.getPictureData().getData()))) {
                        continue;
                    }
                }

                HSSFClientAnchor anchor = (HSSFClientAnchor) pic.getAnchor();
                if (anchor != null) this.imageToMerged(sheet, anchor);
            }

            // 仅第一个 sheet
            break;
        }
    }

    /**
     * 放大图片至合并单元格
     *
     * @param sheet
     * @param anchor
     */
    private void imageToMerged(Sheet sheet, ClientAnchor anchor) {
        int row1 = anchor.getRow1();
        int col1 = anchor.getCol1();

        // 找“图片左上角所在单元格”归属的合并区域
        CellRangeAddress merged = findMergedRegion(sheet, row1, col1);
        if (merged == null) return;

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

    private CellRangeAddress findMergedRegion(Sheet sheet, int r, int c) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress a = sheet.getMergedRegion(i);
            if (a.isInRange(r, c)) return a;
        }
        return null;
    }
}
