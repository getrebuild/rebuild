/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 曲线图
 *
 * @author devezhao
 * @since 12/15/2018
 */
public class LineChart extends ChartData {

    protected LineChart(JSONObject config) {
        super(config);
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSON build() {
        JSONObject renderOption = config.getJSONObject("option");
        if (renderOption == null) renderOption = new JSONObject();

        Dimension[] dims = getDimensions();
        Numerical[] nums = getNumericals();
        final Dimension dim1 = dims[0];

        // 部分支持
        final Type dim1Type = dim1.getField().getType();
        final FormatCalc dim1Calc = dim1.getFormatCalc();
        boolean dateContinuous = renderOption.getBooleanValue("dateContinuous")
                && (dim1Type == FieldType.DATE || dim1Type == FieldType.TIMESTAMP)
                && (dim1Calc == FormatCalc.Y || dim1Calc == FormatCalc.M || dim1Calc == FormatCalc.D || dim1Calc == FormatCalc.Q);

        List<String> dimAxis = new ArrayList<>();
        JSONArray yyyAxis = new JSONArray();
        List<String> dataFlags = new ArrayList<>();

        // 2DIM + 1NUM
        // FIXME 多余AXIS会舍弃
        if (dims.length > 1) {
            Numerical num1 = nums[0];
            Object[][] dataRaw = createQuery(buildSql(dims, num1)).array();
            // 连续日期
            if (dateContinuous && dataRaw.length > 0) {
                dataRaw = putFullDates2Data(dataRaw, dim1, 2);
            }

            List<Object> dim1Set = new ArrayList<>();
            Map<Object, List<Object[]>> dim2Group = new LinkedHashMap<>();
            for (Object[] o : dataRaw) {
                // xAxis
                Object dim1ValueKey = o[0] == null ? ChartsHelper.VALUE_NONE : o[0];
                if (!dim1Set.contains(dim1ValueKey)) dim1Set.add(dim1ValueKey);

                // yAxis
                Object dim2ValueKey = o[1] == null ? ChartsHelper.VALUE_NONE : o[1];
                List<Object[]> same2 = dim2Group.getOrDefault(dim2ValueKey, new ArrayList<>());
                same2.add(o);
                dim2Group.put(dim2ValueKey, same2);
            }

            // xAxis
            for (Object dim1ValueKey : dim1Set) {
                dimAxis.add(wrapAxisValue(dim1, dim1ValueKey));
            }

            final String num1Flag = getNumericalFlag(num1);
            final int dim1SetLen = dim1Set.size();

            // yAxis
            for (Map.Entry<Object, List<Object[]>> e : dim2Group.entrySet()) {
                Object[] yAxis = new Object[dim1SetLen];
                Arrays.fill(yAxis, 0);

                Object dim2ValueKey = e.getKey();
                List<Object[]> list = e.getValue();

                for (int i = 0; i < dim1SetLen; i++) {
                    // Found by dim1 + dim2
                    Object dim1Key = dim1Set.get(i);

                    for (Object[] o : list) {
                        Object dim1Key2 = o[0] == null ? ChartsHelper.VALUE_NONE : o[0];
                        Object dim2Key2 = o[1] == null ? ChartsHelper.VALUE_NONE : o[1];
                        if (dim1Key.equals(dim1Key2) && dim2ValueKey.equals(dim2Key2)) {
                            yAxis[i] = wrapAxisValue(num1, o[2]);
                        }
                    }
                }

                JSONObject map = new JSONObject();
                map.put("name", wrapAxisValue(dims[1], dim2ValueKey));
                map.put("data", yAxis);
                yyyAxis.add(map);
                dataFlags.add(num1Flag);
            }
        }
        // 1DIM + 多NUM
        else {
            Object[][] dataRaw = createQuery(buildSql(dim1, nums)).array();
            // 连续日期
            if (dateContinuous && dataRaw.length > 0) {
                dataRaw = putFullDates2Data(dataRaw, dim1, 1);
            }

            Object[] numsAxis = new Object[nums.length];
            for (Object[] o : dataRaw) {
                dimAxis.add(wrapAxisValue(dim1, o[0]));

                for (int i = 0; i < nums.length; i++) {
                    List<String> numAxis = (List<String>) numsAxis[i];
                    if (numAxis == null) {
                        numAxis = new ArrayList<>();
                        numsAxis[i] = numAxis;
                    }
                    numAxis.add(wrapAxisValue(nums[i], o[i + 1]));
                }
            }

            for (int i = 0; i < nums.length; i++) {
                Numerical axis = nums[i];
                List<String> data = (List<String>) numsAxis[i];

                JSONObject map = new JSONObject();
                map.put("name", axis.getLabel());
                map.put("data", data);
                yyyAxis.add(map);
                dataFlags.add(getNumericalFlag(axis));
            }
        }

        renderOption.put("dataFlags", dataFlags);

        return JSONUtils.toJSONObject(
                new String[]{"xAxis", "yyyAxis", "_renderOption"},
                new Object[]{JSON.toJSON(dimAxis), JSON.toJSON(yyyAxis), renderOption});
    }

    private Object[][] putFullDates2Data(Object[][] dataRaw, Dimension date1, int numStarts) {
        Date min = null;
        Date max = null;
        for (Object[] o : dataRaw) {
            String date2str = o[0] == null ? null : o[0].toString();
            if (StringUtils.isBlank(date2str)) continue;

            String[] isQuarter = date2str.split(" Q");
            if (isQuarter.length == 2) {
                int m = ObjectUtils.toInt(isQuarter[1]) * 3;
                date2str = isQuarter[0] + "-" + StringUtils.leftPad(m + "", 2, "0");
            }

            String format = CalendarUtils.UTC_DATETIME_FORMAT.substring(0, date2str.length());
            Date date = CalendarUtils.parse(date2str, format);

            if (max == null || date.getTime() > max.getTime()) max = date;
            if (min == null || date.getTime() < min.getTime()) min = date;
        }

        if (min == null) return dataRaw;

        final List<Object> emptyRow = new ArrayList<>();
        emptyRow.add(null);
        if (numStarts == 2) emptyRow.add(dataRaw[0][1]);  // dim2
        for (int i = numStarts; i < 9; i++) emptyRow.add(0);

        List<String> fullDates = getFullDates(min, max, date1.getFormatCalc(), date1.getFormatSort());
        List<Object[]> dataRaw2 = new ArrayList<>();
        boolean fullingNull = false;
        for (String date : fullDates) {
            boolean dateMiss = true;
            for (Object[] o : dataRaw) {
                if (o[0] == null) {
                    if (!fullingNull) {
                        fullingNull = true;
                        dataRaw2.add(o);
                        dateMiss = false;
                    }
                } else if (o[0].equals(date)) {
                    dataRaw2.add(o);
                    dateMiss = false;
                }
            }

            if (dateMiss) {
                Object[] emptyRow2 = emptyRow.toArray(new Object[0]);
                emptyRow2[0] = date;
                dataRaw2.add(emptyRow2);
            }
        }

        dataRaw = dataRaw2.toArray(new Object[0][]);
        return dataRaw;
    }

    @SuppressWarnings("deprecation")
    private List<String> getFullDates(Date min, Date max, FormatCalc calc, FormatSort sort) {
        List<Date> fullDates = new ArrayList<>();
        Date temp = min;
        while (temp.getTime() < max.getTime()) {
            fullDates.add(temp);

            if (calc == FormatCalc.D) {
                temp = CalendarUtils.add(temp, 1, Calendar.DAY_OF_MONTH);
            } else {
                temp = CalendarUtils.add(temp, 1, Calendar.MONTH);
            }
        }
        fullDates.add(max);

        // 降序
        if (sort == FormatSort.DESC) Collections.reverse(fullDates);

        List<String> fullDates2 = new ArrayList<>();
        for (Date date : fullDates) {
            String date2unit;
            if (calc == FormatCalc.Y || calc == FormatCalc.Q) {
                date2unit = CalendarUtils.getDateFormat("yyyy").format(date);

                if (calc == FormatCalc.Q) {
                    int m = date.getMonth() + 1;
                    if (m <= 3) date2unit += " Q1";
                    else if (m <= 6) date2unit += " Q2";
                    else if (m <= 9) date2unit += " Q3";
                    else date2unit += " Q4";
                }
            } else if (calc == FormatCalc.M) {
                date2unit = CalendarUtils.getDateFormat("yyyy-MM").format(date);
            } else {
                date2unit = CalendarUtils.getDateFormat("yyyy-MM-dd").format(date);
            }

            if (!fullDates2.contains(date2unit)) fullDates2.add(date2unit);
        }
        return fullDates2;
    }
}
