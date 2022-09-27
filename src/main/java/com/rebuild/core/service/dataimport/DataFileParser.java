/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

import cn.devezhao.commons.excel.CSVReader;
import cn.devezhao.commons.excel.Cell;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.ExcelUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
        this(sourceFile, AppUtils.UTF8);
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
     * 此方法性能较低
     *
     * @return
     * @see #parse()
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

        // GBK/UTF-8
        String enc = getFileCharsetName(this.sourceFile);
        if (!"GBK".equals(enc)) enc = this.encoding;

        try (CSVReader csvReader = new CSVReader(this.sourceFile, enc)) {
            while (csvReader.hasNext()) {
                rows.add(csvReader.next());
                if (rows.size() >= maxRows) {
                    break;
                }
            }
        }
        return rows;
    }

    private static String getFileCharsetName(File file) {
        String name = "GBK";  // GB2312/ANSI
        try (InputStream is = Files.newInputStream(file.toPath())) {
            byte[] head = new byte[3];
            int r = is.read(head);
            if (r == -1) return name;

            if (head[0] == -1 && head[1] == -2) {  // 0xFFFE
                name = "UTF-16";
            } else if (head[0] == -2 && head[1] == -1) {  // 0xFEFF
                name = "Unicode";  // UCS2-Big-Endian/UCS2-Little-Endian
            } else if (head[0] == -27 && head[1] == -101 && head[2] == -98) {
                name = "UTF-8";  // UTF-8
            } else if (head[0] == -17 && head[1] == -69 && head[2] == -65) {
                name = "UTF-8"; // UTF-8-BOM
            }
        } catch (IOException e) {
            return null;
        }
        return name;
    }
}
