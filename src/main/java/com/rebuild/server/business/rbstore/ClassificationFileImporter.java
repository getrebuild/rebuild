/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.rbstore;

import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.business.dataimport.DataFileParser;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.List;

/**
 * 从文件导入
 *
 * @author ZHAO
 * @since 2020/5/12
 */
public class ClassificationFileImporter extends ClassificationImporter {

    final private File file;

    /**
     * @param dest
     * @param file
     */
    public ClassificationFileImporter(ID dest, File file) {
        super(dest, null);
        this.file = file;
    }

    @Override
    protected Integer exec() throws Exception {
        List<Cell[]> rows = new DataFileParser(file).parse();
        this.setTotal(rows.size() - 1);

        boolean first = true;
        for (Cell[] row : rows) {
            if (first) {
                first = false;
                continue;
            }
            if (row.length == 0) {
                continue;
            }

            this.addCompleted();

            String L1 = row[0].asString();
            if (StringUtils.isBlank(L1)) {
                continue;
            }
            ID L1Id = findOrCreate(L1, null, null, LEVEL_BEGIN);

            String L2 = row.length > 1 ? row[1].asString() : null;
            if (StringUtils.isBlank(L2)) {
                continue;
            }
            ID L2Id = findOrCreate(L2, null, L1Id, LEVEL_BEGIN + 1);

            String L3 = row.length > 2 ? row[2].asString() : null;
            if (StringUtils.isBlank(L3)) {
                continue;
            }
            ID L3Id = findOrCreate(L3, null, L2Id, LEVEL_BEGIN + 2);

            String L4 = row.length > 3 ? row[3].asString() : null;
            if (StringUtils.isBlank(L4)) {
                continue;
            }
            findOrCreate(L4, null, L3Id, LEVEL_BEGIN + 3);
        }

        return this.getSucceeded();
    }
}
