/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.excel.IRow;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;
import org.apache.poi.xssf.usermodel.XSSFTextRun;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
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
    void testShape() throws IOException {
        try (Workbook wb = WorkbookFactory.create(new File("/Users/zhaoff/Desktop/1.xlsx"))) {
            Sheet sheet = wb.getSheetAt(0);
            for (Object o : sheet.getDrawingPatriarch()) {
                XSSFSimpleShape shape = (XSSFSimpleShape) o;
                XSSFTextRun s = shape.getTextParagraphs().get(0).getTextRuns().get(0);

                XSSFFont font = (XSSFFont) wb.createFont();
                font.setFontName(s.getFontFamily());
                font.setFontHeightInPoints((short) s.getFontSize());
                font.setBold(s.isBold());
                font.setItalic(s.isItalic());

                XSSFRichTextString richTextString = new XSSFRichTextString("11");
                richTextString.applyFont(font);
            }
        }
    }
}