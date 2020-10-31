/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.SetUser;
import com.rebuild.core.support.general.DataListBuilderImpl;
import com.rebuild.core.support.general.DataListWrapper;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据导出
 *
 * @author ZHAO
 * @see DataListBuilderImpl
 * @since 2019/11/18
 */
public class DataExporter extends SetUser {

    /**
     * 最大行数
     */
    public static final int MAX_ROWS = 65535 - 1;

    final private JSONObject queryData;
    // 字段
    private List<Field> headFields = new ArrayList<>();

    /**
     * @param queryData
     */
    public DataExporter(JSONObject queryData) {
        this.queryData = queryData;
    }

    /**
     * 导出
     *
     * @return
     */
    public File export() {
        File tmp = RebuildConfiguration.getFileOfTemp(String.format("EXPORT-%d.csv", System.currentTimeMillis()));
        export(tmp);
        return tmp;
    }

    /**
     * 导出到指定文件
     *
     * @param dest
     */
    public void export(File dest) {
        DataListBuilderImpl control = new DataListBuilderImpl(queryData, getUser());

        List<String> head = this.buildHead(control);

        try (FileOutputStream fos = new FileOutputStream(dest, true)) {
            try (OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                try (BufferedWriter writer = new BufferedWriter(osw)) {
                    writer.write("\ufeff");
                    writer.write(mergeLine(head));

                    for (List<String> row : this.buildData(control)) {
                        writer.newLine();
                        writer.write(mergeLine(row));
                    }

                    writer.flush();
                }
            }
        } catch (IOException e) {
            throw new RebuildException("Cannot write .csv file", e);
        }
    }

    private String mergeLine(List<String> line) {
        StringBuilder sb = new StringBuilder();
        boolean b = true;
        for (String s : line) {
            if (b) b = false;
            else sb.append(", ");

            if (s.contains(",")) sb.append("\"").append(s).append("\"");
            else sb.append(s);
        }
        return sb.toString();
    }

    /**
     * 表头
     *
     * @param control
     * @return
     */
    protected List<String> buildHead(DataListBuilderImpl control) {
        List<String> headList = new ArrayList<>();
        for (String field : control.getQueryParser().getQueryFields()) {
            headFields.add(MetadataHelper.getLastJoinField(control.getEntity(), field));
            String fieldLabel = EasyMeta.getLabel(control.getEntity(), field);
            headList.add(fieldLabel);
        }
        return headList;
    }

    /**
     * 数据
     *
     * @param control
     * @return
     */
    protected List<List<String>> buildData(DataListBuilderImpl control) {
        JSONArray data = ((JSONObject) control.getJSONResult()).getJSONArray("data");

        List<List<String>> into = new ArrayList<>();
        for (Object row : data) {
            JSONArray rowJson = (JSONArray) row;

            int cellIndex = 0;
            List<String> cellVals = new ArrayList<>();
            for (Object cellVal : rowJson) {
                // 最后添加的记录 ID
                // 详情可见 QueryParser#doParseIfNeed (L171)
                if (cellIndex >= headFields.size()) {
                    break;
                }

                Field field = headFields.get(cellIndex++);
                DisplayType dt = EasyMeta.getDisplayType(field);
                if (cellVal == null) {
                    cellVal = StringUtils.EMPTY;
                } else if (dt == DisplayType.FILE || dt == DisplayType.IMAGE || dt == DisplayType.AVATAR
                        || dt == DisplayType.ANYREFERENCE || dt == DisplayType.BARCODE) {
                    cellVal = "[暂不支持" + dt.getDisplayName() + "字段]";
                } else if (dt == DisplayType.DECIMAL || dt == DisplayType.NUMBER) {
                    cellVal = cellVal.toString().replace(",", "");  // 移除千分位
                }

                if (cellVal instanceof JSONObject) {
                    cellVal = ((JSONObject) cellVal).getString("text");
                } else if (cellVal.toString().equals(DataListWrapper.NO_READ_PRIVILEGES)) {
                    cellVal = "[无权限]";
                }
                cellVals.add(cellVal.toString());
            }
            into.add(cellVals);
        }
        return into;
    }
}
