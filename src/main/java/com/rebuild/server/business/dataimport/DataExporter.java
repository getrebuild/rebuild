/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.dataimport;

import cn.devezhao.persist4j.Field;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.helper.SetUser;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.datalist.DataListWrapper;
import com.rebuild.server.helper.datalist.DefaultDataListControl;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数据导出
 *
 * @author ZHAO
 * @since 2019/11/18
 * @see DefaultDataListControl
 */
public class DataExporter extends SetUser<DataExporter> {

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
        File tmp = SysConfiguration.getFileOfTemp(String.format("EXPORT-%d.xls", System.currentTimeMillis()));
        export(tmp);
        return tmp;
    }

    /**
     * 导出到指定文件
     *
     * @param dest
     */
    public void export(File dest) {
        DefaultDataListControl control = new DefaultDataListControl(queryData, getUser());
        EasyExcel.write(dest)
                .registerWriteHandler(new ColumnWidthStrategy())
                .registerWriteHandler(this.buildStyle())
                .sheet(EasyMeta.getLabel(control.getEntity()))
                .head(this.buildHead(control))
                .doWrite(this.buildData(control));
    }

    /**
     * 表头
     *
     * @param control
     * @return
     */
    protected List<List<String>> buildHead(DefaultDataListControl control) {
        List<List<String>> headList = new ArrayList<>();
        for (String field : control.getQueryParser().getQueryFields()) {
            headFields.add(MetadataHelper.getLastJoinField(control.getEntity(), field));
            String fieldLabel = EasyMeta.getLabel(control.getEntity(), field);
            headList.add(Collections.singletonList(fieldLabel));
        }
        return headList;
    }

    /**
     * 数据
     *
     * @param control
     * @return
     */
    protected List<List<Object>> buildData(DefaultDataListControl control) {
        JSONArray data = ((JSONObject) control.getJSONResult()).getJSONArray("data");

        List<List<Object>> into = new ArrayList<>();
        for (Object row : data) {
            JSONArray rowJson = (JSONArray) row;

            int cellIndex = 0;
            List<Object> cellVals = new ArrayList<>();
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
                        || dt == DisplayType.ANYREFERENCE) {
                    cellVal = "[暂不支持" + dt.getDisplayName() + "字段]";
                } else if (dt == DisplayType.DECIMAL || dt == DisplayType.NUMBER) {
                    cellVal = cellVal.toString().replace(",", "");  // 移除千分位
                }

                if (cellVal instanceof JSONObject) {
                    cellVal = ((JSONObject) cellVal).getString("text");
                } else if (cellVal.toString().equals(DataListWrapper.NO_READ_PRIVILEGES)) {
                    cellVal = "[无权限]";
                }
                cellVals.add(cellVal);
            }
            into.add(cellVals);
        }
        return into;
    }

    /**
     * 样式
     *
     * @return
     */
    protected HorizontalCellStyleStrategy buildStyle() {
        WriteFont baseFont = new WriteFont();
        baseFont.setFontHeightInPoints((short) 12);
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

        return new HorizontalCellStyleStrategy(headStyle, contentStyle);
    }
}
