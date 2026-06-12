/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.data.DataFormatData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.DefinedException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyDecimal;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.MediaValue;
import com.rebuild.core.metadata.easymeta.MixValue;
import com.rebuild.core.metadata.easymeta.MultiValue;
import com.rebuild.core.service.datareport.EasyExcelGenerator;
import com.rebuild.core.service.datareport.EasyExcelListGenerator;
import com.rebuild.core.service.datareport.ReportsException;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.SetUser;
import com.rebuild.core.support.general.DataListBuilderImpl;
import com.rebuild.core.support.general.DataListWrapper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数据导出
 *
 * @author ZHAO
 * @since 2019/11/18
 *
 * @see DataListBuilderImpl
 * @see EasyExcelGenerator
 */
public class DataExporter extends SetUser {

    /**
     * 最大导出行数
     */
    public static final int MAX_ROWS = 1000000;
    
    final private JSONObject queryData;
    // 字段
    private List<Field> headFields = new ArrayList<>();

    private int count = 0;

    /**
     * @param queryData
     */
    public DataExporter(JSONObject queryData) {
        this.queryData = queryData;
        this.queryData.put("reload", false);
        this.queryData.put("statsField", false);
    }

    /**
     * 通过模板导出
     *
     * @param useReport
     * @return
     * @see com.rebuild.core.service.datareport.EasyExcelListGenerator
     */
    public File export(ID useReport) {
        EasyExcelListGenerator g = EasyExcelListGenerator.create(useReport, this.queryData);
        g.setUser(getUser());
        File file = g.generate();
        count = g.getExportCount();
        return file;

//        return new MuiltSheetExcelGenerator(useReport, queryData).generate();
    }

    /**
     * 导出 CSV 或 Excel
     *
     * @return
     */
    public File export(String csvOrExcel) {
        final DataListBuilderImpl builder = new DataListBuilderImpl2(queryData, getUser());
        final List<String> head = this.buildHead(builder);

        // Excel
        // xlsx: 无 65535 行数限制
        if ("xls".equalsIgnoreCase(csvOrExcel) || "xlsx".equalsIgnoreCase(csvOrExcel)) {
            File file = RebuildConfiguration.getFileOfTemp(
                    String.format("RBEXPORT-%d.%s", System.currentTimeMillis(), csvOrExcel.toLowerCase()));

            List<List<String>> head4Excel = new ArrayList<>();
            for (String h : head) {
                head4Excel.add(Collections.singletonList(h));
            }

            List<List<Object>> datas = this.buildData(builder, false);
            if (datas.isEmpty()) throw new DefinedException(Language.L("暂无数据"));

            EasyExcel.write(file)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .registerWriteHandler(this.buildExcelStyle())
                    .sheet("Sheet1")
                    .head(head4Excel)
                    .doWrite(datas);
            this.count = datas.size();
            return file;
        }

        // CSV

        File file = RebuildConfiguration.getFileOfTemp(String.format("RBEXPORT-%d.csv", System.currentTimeMillis()));
        try (FileOutputStream fos = new FileOutputStream(file, Boolean.TRUE)) {
            try (OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                try (BufferedWriter writer = new BufferedWriter(osw)) {
                    writer.write("\ufeff");
                    writer.write(mergeLine(head));

                    List<List<Object>> datas = this.buildData(builder, true);
                    if (datas.isEmpty()) throw new DefinedException(Language.L("暂无数据"));

                    for (List<Object> row : datas) {
                        writer.newLine();
                        writer.write(mergeLine(row));
                        count++;
                    }

                    writer.flush();
                }
            }
        } catch (IOException e) {
            throw new ReportsException("Cannot write .csv file", e);
        }
        return file;
    }

    private String mergeLine(List<?> line) {
        StringBuilder sb = new StringBuilder();
        boolean b = true;
        for (Object s : line) {
            if (b) b = false;
            else sb.append(",");

            sb.append(s == null ? "" : s.toString().replace(", ", " / "));
        }
        return sb.toString();
    }

    /**
     * 表头
     *
     * @param builder
     * @return
     */
    protected List<String> buildHead(DataListBuilderImpl builder) {
        List<String> headList = new ArrayList<>();
        for (String field : builder.getQueryParser().getQueryFields()) {
            headFields.add(MetadataHelper.getLastJoinField(builder.getEntity(), field));
            String fieldLabel = EasyMetaFactory.getLabel(builder.getEntity(), field);
            headList.add(fieldLabel);
        }
        return headList;
    }

    /**
     * 內容
     *
     * @param builder
     * @param isCvs
     * @return
     */
    protected List<List<Object>> buildData(DataListBuilderImpl builder, boolean isCvs) {
        final JSONArray data = ((JSONObject) builder.getJSONResult()).getJSONArray("data");

        final String labelNop = Language.L("[无权限]");
        final String labelUns = Language.L("[暂不支持]");

        List<List<Object>> dataList = new ArrayList<>();
        for (Object row : data) {
            JSONArray rowJson = (JSONArray) row;

            int cellIndex = 0;
            List<Object> cellVals = new ArrayList<>();
            for (Object cellVal : rowJson) {
                // 最后添加的记录 ID
                // 详情可见 QueryParser#doParseIfNeed:L171
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
                    cellVal = labelNop;
                } else if (!dt.isExportable() || easyField instanceof MediaValue) {
                    cellVal = labelUns;
                } else if (dt == DisplayType.DECIMAL || dt == DisplayType.NUMBER) {
                    cellVal = cellVal.toString().replaceAll("[^0-9|^.-]", "");  // 仅保留数字

                    if (dt == DisplayType.DECIMAL) {
                        cellVal = EasyDecimal.fixedDecimalScale(cellVal, easyField);

                        // 单元格格式
                        WriteCellData<Object> wcd = new WriteCellData<>();
                        wcd.setType(CellDataTypeEnum.NUMBER);
                        wcd.setNumberValue((BigDecimal) cellVal);
                        WriteCellStyle style = new WriteCellStyle();
                        DataFormatData format = new DataFormatData();
                        format.setFormat("0." + "0000000000".substring(0, ((EasyDecimal) easyField).getScale()));
                        style.setDataFormatData(format);
                        wcd.setWriteCellStyle(style);
                        cellVal = wcd;
                    } else {
                        cellVal = ObjectUtils.toLong(cellVal);
                    }

                } else if (dt == DisplayType.ID) {
                    cellVal = ((JSONObject) cellVal).getString("id");
                }

                if (easyField instanceof MixValue &&
                        (cellVal instanceof JSONObject || cellVal instanceof JSONArray)) {
                    cellVal = ((MixValue) easyField).unpackWrapValue(cellVal);

                    if (isCvs && cellVal.toString().contains(", ") && easyField instanceof MultiValue) {
                        cellVal = cellVal.toString().replace(", ", " / ");
                    }
                }

                cellVals.add(cellVal);
            }
            dataList.add(cellVals);
        }
        return dataList;
    }

    /**
     * Excel 样式
     *
     * @return
     */
    private HorizontalCellStyleStrategy buildExcelStyle() {
        WriteFont baseFont = new WriteFont();
        baseFont.setFontHeightInPoints((short) 11);
        baseFont.setColor(IndexedColors.BLACK.getIndex());

        // 头
        WriteCellStyle headStyle = new WriteCellStyle();
        headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headStyle.setWriteFont(baseFont);
        // 内容
        WriteCellStyle contentStyle = new WriteCellStyle();
        contentStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        // 这里需要指定 FillPatternType 为 FillPatternType.SOLID_FOREGROUND 不然无法显示背景颜色
        // 头默认了 FillPatternType 所以可以不指定
        contentStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        contentStyle.setWriteFont(baseFont);
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);
        // 换行
        contentStyle.setWrapped(true);
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        contentStyle.setHorizontalAlignment(HorizontalAlignment.LEFT);

        return new HorizontalCellStyleStrategy(headStyle, contentStyle);
    }

    public int getExportCount() {
        return count;
    }

    // Simplify
    static class DataListBuilderImpl2 extends DataListBuilderImpl {
        DataListBuilderImpl2(JSONObject query, ID user) {
            super(query, user);
        }

        @Override
        protected boolean isNeedReload() {
            return false;
        }

        @Override
        protected DataListWrapper createDataListWrapper(int totalRows, Object[][] data, Query query) {
            DataListWrapper wrapper = super.createDataListWrapper(totalRows, data, query);
            wrapper.setMixWrapper(false);
            return wrapper;
        }
    }
}
