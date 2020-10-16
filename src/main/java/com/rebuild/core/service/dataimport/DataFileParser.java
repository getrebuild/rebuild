/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

import cn.devezhao.commons.excel.CSVReader;
import cn.devezhao.commons.excel.Cell;
import com.rebuild.utils.ExcelUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件解析
 *
 * @author devezhao
 * @since 01/09/2019
 */
public class DataFileParser {

    final private File sourceFile;
    final private String encoding;

    /**
     * @param sourceFile
     */
    public DataFileParser(File sourceFile) {
        this(sourceFile, "utf-8");
    }

    /**
     * @param sourceFile
     * @param encoding
     */
    public DataFileParser(File sourceFile, String encoding) {
        this.sourceFile = sourceFile;
        this.encoding = encoding;
    }

    /**
     * @return
     */
    public File getSourceFile() {
        return sourceFile;
    }

    /**
     * 此方法性能较低，与 {@link #parse()} 差不多
     *
     * @return
     */
    public int getRowsCount() {
        return parse().size();
    }

    /**
     * @return
     */
    public List<Cell[]> parse() {
        return parse(Integer.MAX_VALUE);
    }

    /**
     * @param maxRows
     * @return
     */
    public List<Cell[]> parse(int maxRows) {
        if (sourceFile.getName().endsWith(".csv")) {
            return parseCsv(maxRows);
        } else {
            return ExcelUtils.readExcel(this.sourceFile, maxRows, true);
        }
    }

    /**
     * @param maxRows
     * @return
     */
    private List<Cell[]> parseCsv(int maxRows) {
        final List<Cell[]> rows = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(this.sourceFile, this.encoding)) {
            while (csvReader.hasNext()) {
                rows.add(csvReader.next());
                if (rows.size() >= maxRows) {
                    break;
                }
            }
        }
        return rows;
    }
}
