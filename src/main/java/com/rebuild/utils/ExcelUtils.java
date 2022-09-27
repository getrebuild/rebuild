/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.excel.Cell;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.rebuild.core.RebuildException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Excel 工具，封装 easyexcel
 *
 * @author devezhao
 * @since 2020/7/15
 */
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
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                    }
                }).sheet().doRead();
            }

        } catch (IOException e) {
            throw new RebuildException(e);
        }
        return rows;
    }
}
