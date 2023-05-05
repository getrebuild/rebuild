/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.service.dataimport.DataFileParser;
import com.rebuild.utils.CommonsUtils;
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
            if (row.length == 0) continue;

            this.addCompleted();

            String L1 = row[0].asString();
            if (StringUtils.isBlank(L1)) continue;
            ID L1Id = findOrCreate2(L1, null, LEVEL_BEGIN);

            String L2 = row.length > 1 ? row[1].asString() : null;
            if (StringUtils.isBlank(L2)) continue;
            ID L2Id = findOrCreate2(L2, L1Id, LEVEL_BEGIN + 1);

            String L3 = row.length > 2 ? row[2].asString() : null;
            if (StringUtils.isBlank(L3)) continue;
            ID L3Id = findOrCreate2(L3, L2Id, LEVEL_BEGIN + 2);

            String L4 = row.length > 3 ? row[3].asString() : null;
            if (StringUtils.isBlank(L4)) continue;
            findOrCreate2(L4, L3Id, LEVEL_BEGIN + 3);
        }

        return this.getSucceeded();
    }

    private ID findOrCreate2(String name, ID parent, int level) {
        // NAME$$$$CODE
        String[] s = name.split(CommonsUtils.COMM_SPLITER_RE);
        return findOrCreate(s[0], s.length >= 2 ? s[1] : null, parent, level);
    }
}
