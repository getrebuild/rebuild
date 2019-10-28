/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.business.dataimport;

import cn.devezhao.commons.excel.CSVReader;
import cn.devezhao.commons.excel.Cell;
import com.rebuild.utils.CommonsUtils;

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
        this.sourceFile = sourceFile;
        this.encoding = "GBK";
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
            return CommonsUtils.readExcel(this.sourceFile, maxRows, true);
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
