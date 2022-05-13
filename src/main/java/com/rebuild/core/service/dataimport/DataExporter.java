/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.*;
import com.rebuild.core.service.datareport.EasyExcelListGenerator;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.SetUser;
import com.rebuild.core.support.general.DataListBuilderImpl;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
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

    private int count = 0;

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
        return export(null);
    }

    /**
     * 通过模板导出
     *
     * @param useReport
     * @return
     * @see com.rebuild.core.service.datareport.EasyExcelListGenerator
     */
    public File export(ID useReport) {
        if (useReport == null) {
            File tmp = RebuildConfiguration.getFileOfTemp(String.format("RBEXPORT-%d.csv", System.currentTimeMillis()));
            exportCsv(tmp);
            return tmp;
        } else {
            EasyExcelListGenerator generator = new EasyExcelListGenerator(useReport, this.queryData);
            generator.setUser(getUser());
            File file = generator.generate();

            count = generator.getExportCount();
            return file;
        }
    }

    /**
     * 导出到指定文件
     *
     * @param dest
     */
    protected void exportCsv(File dest) {
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
                        count++;
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
            else sb.append(",");

            sb.append(s);
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
            String fieldLabel = EasyMetaFactory.getLabel(control.getEntity(), field);
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
                EasyField easyField = EasyMetaFactory.valueOf(field);
                DisplayType dt = easyField.getDisplayType();

                if (cellVal == null) {
                    cellVal = StringUtils.EMPTY;
                }

                if (cellVal.toString().equals(FieldValueHelper.NO_READ_PRIVILEGES)) {
                    cellVal = Language.L("[无权限]");
                } else if (!dt.isExportable() || (dt == DisplayType.SIGN || dt == DisplayType.BARCODE)) {
                    cellVal = Language.L("[暂不支持]");
                } else if (dt == DisplayType.DECIMAL || dt == DisplayType.NUMBER) {
                    cellVal = cellVal.toString().replace(",", "");  // 移除千分位
                } else if (dt == DisplayType.ID) {
                    cellVal = ((JSONObject) cellVal).getString("id");
                }

                if (easyField instanceof MixValue &&
                        (cellVal instanceof JSONObject || cellVal instanceof JSONArray)) {
                    cellVal = ((MixValue) easyField).unpackWrapValue(cellVal);

                    if (cellVal.toString().contains(", ")
                            && (easyField instanceof EasyMultiSelect || easyField instanceof EasyN2NReference)) {
                        cellVal = cellVal.toString().replace(", ", " / ");
                    }
                }

                cellVals.add(cellVal.toString());
            }
            into.add(cellVals);
        }
        return into;
    }

    public int getExportCount() {
        return count;
    }
}
