/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.excel.Cell;
import cn.devezhao.commons.excel.IRow;
import cn.hutool.core.io.file.FileWriter;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.rebuild.core.RebuildException;
import com.rebuild.core.service.dataimport.DataFileParser;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Excel 工具，封装 easyexcel
 *
 * @author devezhao
 * @since 2020/7/15
 */
@Slf4j
public class ExcelUtils {

    public static final int MAX_UNLIMIT = -1;

    /**
     * @param excel
     * @return
     */
    public static List<Cell[]> readExcel(File excel) {
        return readExcel(excel, MAX_UNLIMIT, true);
    }

    /**
     * @param excel
     * @param maxRows
     * @param hasHead
     * @return
     */
    public static List<Cell[]> readExcel(File excel, int maxRows, boolean hasHead) {
        final List<Cell[]> rows = new ArrayList<>();
        final AtomicInteger rowNo = new AtomicInteger(0);

        try (InputStream is = Files.newInputStream(excel.toPath())) {
            try (BufferedInputStream bis = new BufferedInputStream(is)) {
                // noinspection rawtypes
                EasyExcel.read(bis, null, new AnalysisEventListener() {
                    @Override
                    public void invokeHeadMap(Map headMap, AnalysisContext context) {
                        if (hasHead) {
                            this.invoke(headMap, context);
                        } else {
                            rowNo.incrementAndGet();
                        }
                    }

                    @Override
                    public void invoke(Object data, AnalysisContext analysisContext) {
                        if (maxRows > 0 && rows.size() >= maxRows) {
                            return;
                        }

                        @SuppressWarnings("unchecked")
                        Map<Integer, String> dataMap = (Map<Integer, String>) data;
                        List<Cell> row = new ArrayList<>();
                        for (int i = 0; i < dataMap.size(); i++) {
                            row.add(new Cell(dataMap.get(i), rowNo.get(), i));
                        }

                        rows.add(row.toArray(new Cell[0]));
                        rowNo.incrementAndGet();
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {}
                }).sheet().doRead();
            }

        } catch (IOException e) {
            throw new RebuildException(e);
        }
        return rows;
    }

    /**
     * @param excel
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static IRow[] readExcelRows(File excel) {
        final List<IRow> rows = new ArrayList<>();
        final AtomicInteger rowNo = new AtomicInteger(0);

        try (InputStream is = Files.newInputStream(excel.toPath())) {
            try (BufferedInputStream bis = new BufferedInputStream(is)) {
                EasyExcel.read(bis, null, new AnalysisEventListener() {
                    @Override
                    public void invokeHeadMap(Map headMap, AnalysisContext context) {
                        this.invoke(headMap, context);
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public void invoke(Object data, AnalysisContext analysisContext) {
                        Map<Integer, String> dataMap = (Map<Integer, String>) data;
                        List<Cell> row = new ArrayList<>();
                        for (int i = 0; i < dataMap.size(); i++) {
                            row.add(new Cell(dataMap.get(i), rowNo.get(), i));
                        }

                        rows.add(new IRow(row.toArray(new Cell[0]), rowNo.get()));
                        rowNo.incrementAndGet();
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {}
                }).sheet().doRead();
            }

        } catch (IOException e) {
            throw new RebuildException(e);
        }

        return rows.toArray(new IRow[0]);
    }

    /**
     * 打开并保存以便公式生效
     *
     * @param path
     */
    public static boolean reSaveAndCalcFormula(Path path) {
        try (Workbook wb = WorkbookFactory.create(Files.newInputStream(path))) {
            wb.setForceFormulaRecalculation(true);
            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();

            try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
                wb.write(fos);
            }

        } catch (Exception ex) {
            log.error("Re-save excel error : {}", path, ex);
            return false;
        }
        return true;
    }

    /**
     * 保存 Excel 为 CSV
     *
     * @param excelSource
     * @param encoding 默认 UTF-8
     * @return
     */
    public static Path saveToCsv(Path excelSource, String encoding) {
        // 公式生效
        reSaveAndCalcFormula(excelSource);

        File csvFile = RebuildConfiguration.getFileOfTemp(String.format("%s.csv", excelSource.getFileName().toString()));
        if (encoding == null) encoding = AppUtils.UTF8;
        FileWriter fw = new FileWriter(csvFile, encoding);

        // UTF8 BOM
        if (AppUtils.UTF8.equalsIgnoreCase(encoding)) fw.write("\ufeff");

        final List<Cell[]> rows = new DataFileParser(excelSource.toFile()).parse();
        rows.forEach(row -> {
            String rowData = Arrays.stream(row).map(cell -> Optional.ofNullable(cell).map(Cell::asString).orElse("")).collect(Collectors.joining(","));
            fw.write(rowData + System.lineSeparator(), true);
        });
        return csvFile.toPath();
    }
}
