/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import cn.devezhao.commons.ObjectUtils;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parseing `resources/scripts/db-upgrade.sql`
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/22
 */
public class UpgradeScriptReader {

    private static final String TAG_STARTS = "-- #";
    private static final String TAG_COMMENT = "--";

    /**
     * @return
     * @throws IOException
     */
    public Map<Integer, String[]> read() throws IOException {
        List<String> sqlScripts;
        try (InputStream is = CommonsUtils.getStreamOfRes("scripts/db-upgrade.sql")) {
            sqlScripts = IOUtils.readLines(is, "utf-8");
        }

        Map<Integer, String[]> sqls = new HashMap<>();

        int oneVer = -1;
        List<String> sqlBatch = new ArrayList<>();
        StringBuffer sqlOne = new StringBuffer();

        for (String sl : sqlScripts) {
            if (StringUtils.isBlank(sl)) {
                continue;
            }

            if (sl.startsWith(TAG_STARTS)) {
                if (oneVer > -1) {
                    sqls.put(oneVer, sqlBatch.toArray(new String[0]));
                }

                // reset
                String ver = sl.substring(TAG_STARTS.length()).split(" ")[0];  // eg: -- #2 abc
                oneVer = ObjectUtils.toInt(ver);
                sqlBatch = new ArrayList<>();

            } else if (sl.startsWith(TAG_COMMENT)) {
                // IGNORE COMMENTS
            } else {
                sqlOne.append(sl).append("\n");
                if (sl.endsWith(";")) {  // SQL end by `;`
                    sqlBatch.add(sqlOne.toString());
                    sqlOne = new StringBuffer();
                }
            }
        }

        if (sqlOne.length() > 0) {
            sqlBatch.add(sqlOne.toString());
        }
        if (oneVer > -1) {
            sqls.put(oneVer, sqlBatch.toArray(new String[0]));
        }

        return sqls;
    }
}
