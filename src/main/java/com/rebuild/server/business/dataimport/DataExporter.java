/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

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

    private JSONObject query;

    // 字段
    private List<Field> headFields = new ArrayList<>();

    /**
     * @param query
     */
    public DataExporter(JSONObject query) {
        this.query = query;
    }

    /**
     * 导出
     *
     * @return
     */
    public File export() {
        File tmp = SysConfiguration.getFileOfTemp(String.format("数据导出-%d.xls", System.currentTimeMillis()));
        export(tmp);
        return tmp;
    }

    /**
     * 导出到指定文件
     *
     * @param dest
     */
    public void export(File dest) {
        DefaultDataListControl control = new DefaultDataListControl(query, getUser());
        EasyExcel.write(dest)
                .registerWriteHandler(new ColumnWidthStrategy())
                .registerWriteHandler(this.buildStyle())
                .sheet(EasyMeta.getLabel(control.getEntity()) + "列表")
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
            List<Object> cellList = new ArrayList<>();
            for (Object cell : rowJson) {
                // 最后添加的相关记录 ID
                // 详情可见 QueryParser#doParseIfNeed (L171)
                if (cellIndex >= headFields.size()) {
                    break;
                }

                if (cell == null) {
                    cell = StringUtils.EMPTY;
                }

                Field field = headFields.get(cellIndex++);
                DisplayType dt = EasyMeta.getDisplayType(field);
                if (dt == DisplayType.FILE || dt == DisplayType.IMAGE
                        || dt == DisplayType.AVATAR || dt == DisplayType.ANYREFERENCE) {
                    cell = "[暂不支持" + dt.getDisplayName() + "字段]";
                } else if (dt == DisplayType.DECIMAL || dt == DisplayType.NUMBER) {
                    cell = cell.toString().replace(",", "");  // 移除千分位
                }

                if (cell instanceof Object[]) {
                    cellList.add(((Object[]) cell)[1]);
                } else if (cell instanceof JSONArray) {
                    cellList.add(((JSONArray) cell).get(1));
                } else {
                    if (cell.toString().equals(DataListWrapper.NO_READ_PRIVILEGES)) {
                        cell = "[无权限]";
                    }

                    cellList.add(cell);
                }
            }
            into.add(cellList);
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
        baseFont.setFontHeightInPoints((short) 10);
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
