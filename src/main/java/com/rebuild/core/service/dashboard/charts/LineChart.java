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

import java.util.*;

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

    @Override
    public JSON build() {
        JSONObject renderOption = config.getJSONObject("option");
        if (renderOption == null) renderOption = new JSONObject();

        Dimension[] dims = getDimensions();
        Numerical[] nums = getNumericals();
        final Dimension dim1 = dims[0];

        // 日期连续仅部分支持
        final Type dim1Type = dim1.getField().getType();
        final FormatCalc dim1Calc = dim1.getFormatCalc();
        boolean is4Date = (dim1Type == FieldType.DATE || dim1Type == FieldType.TIMESTAMP)
                && (dim1Calc == FormatCalc.Y || dim1Calc == FormatCalc.Q || dim1Calc == FormatCalc.M || dim1Calc == FormatCalc.W || dim1Calc == FormatCalc.D);
        boolean dateContinuous = renderOption.getBooleanValue("dateContinuous") && is4Date;
        // v3.8.3 使用日期对比
        boolean useComparison = renderOption.getBooleanValue("useComparison") && is4Date;

        List<String> dimAxis = new ArrayList<>();
        JSONArray yyyAxis = new JSONArray();
        List<String> dataFlags = new ArrayList<>();

        // 模式1: 2-DIM + 1-NUM
        // FIXME 多余AXIS会舍弃
        if (dims.length > 1) {
            Numerical num1 = nums[0];
            Object[][] dataRaw = createQuery(buildSql(dims, num1)).array();
            // 连续日期
            if (dateContinuous && dataRaw.length > 0) {
                dataRaw = putContinuousDate2Data(dataRaw, dim1, 2);
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
        // 模式2: 1-DIM + N-NUM
        else {
            Object[][] dataRaw;
            if (nums.length > 1 && hasNumericalFilter(nums)) {
                // 分别查询
                List<AxisEntry> axisValues = new ArrayList<>();
                int indexAndSize = 0;
                for (Numerical num : nums) {
                    String sql = buildSql(dim1, new Numerical[]{num}, true);
                    Object[][] array = createQuery(sql).array();
                    // 连续日期
                    if (dateContinuous && array.length > 0) {
                        array = putContinuousDate2Data(array, dim1, 1);
                    }

                    for (Object[] o : array) axisValues.add(new AxisEntry(o, indexAndSize));
                    indexAndSize++;
                }

                dataRaw = mergeAxisEntry2Data(axisValues, indexAndSize, useComparison);
            } else {
                dataRaw = createQuery(buildSql(dim1, nums, true)).array();
                // 连续日期
                if (dateContinuous && dataRaw.length > 0) {
                    dataRaw = putContinuousDate2Data(dataRaw, dim1, 1);
                }
            }

            Object[] numsAxis = new Object[nums.length];
            for (Object[] o : dataRaw) {
                dimAxis.add(wrapAxisValue(dim1, o[0]));

                for (int i = 0; i < nums.length; i++) {
                    @SuppressWarnings("unchecked")
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
                @SuppressWarnings("unchecked")
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

    private Object[][] putContinuousDate2Data(Object[][] dataRaw, Dimension date1, int numStartIndex) {
        Date min = null;
        Date max = null;
        for (Object[] o : dataRaw) {
            String date2str = o[0] == null ? null : o[0].toString();
            if (StringUtils.isBlank(date2str)) continue;

            Date date = null;

            // 2024 Q2
            String[] isQuarter = date2str.split(" Q");
            if (isQuarter.length == 2) {
                int m = ObjectUtils.toInt(isQuarter[1]) * 3;
                date2str = isQuarter[0] + "-" + StringUtils.leftPad(m + "", 2, "0");
            } else {
                // 2024 W04
                String[] isWeek = date2str.split(" W");
                if (isWeek.length == 2) {
                    int m = ObjectUtils.toInt(isWeek[1]);
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.YEAR, ObjectUtils.toInt(isWeek[0]));
                    cal.set(Calendar.WEEK_OF_YEAR, m);
                    date = cal.getTime();
                }
            }

            if (date == null) {
                String format = CalendarUtils.UTC_DATETIME_FORMAT.substring(0, date2str.length());
                date = CalendarUtils.parse(date2str, format);
            }

            if (max == null || date.getTime() > max.getTime()) max = date;
            if (min == null || date.getTime() < min.getTime()) min = date;
        }

        if (min == null) return dataRaw;

        final List<Object> emptyRow = new ArrayList<>();
        emptyRow.add(null);
        if (numStartIndex == 2) emptyRow.add(dataRaw[0][1]);  // dim2
        for (int i = numStartIndex; i < 9; i++) emptyRow.add(0);

        List<String> dates = getContinuousDate(min, max, date1.getFormatCalc(), date1.getFormatSort());
        List<Object[]> dataRawFd = new ArrayList<>();
        boolean fullingNull = false;
        for (String date : dates) {
            boolean dateMiss = true;
            for (Object[] o : dataRaw) {
                if (o[0] == null) {
                    if (!fullingNull) {
                        fullingNull = true;
                        dataRawFd.add(o);
                        dateMiss = false;
                    }
                } else if (o[0].equals(date)) {
                    dataRawFd.add(o);
                    dateMiss = false;
                }
            }

            if (dateMiss) {
                Object[] emptyRow2 = emptyRow.toArray(new Object[0]);
                emptyRow2[0] = date;
                dataRawFd.add(emptyRow2);
            }
        }

        dataRaw = dataRawFd.toArray(new Object[0][]);
        return dataRaw;
    }

    private List<String> getContinuousDate(Date min, Date max, FormatCalc calc, FormatSort sort) {
        List<Date> dates = new ArrayList<>();
        Date temp = min;
        while (temp.getTime() < max.getTime()) {
            dates.add(temp);

            if (calc == FormatCalc.D) {
                temp = CalendarUtils.add(temp, 1, Calendar.DAY_OF_MONTH);
            } else if (calc == FormatCalc.W) {
                temp = CalendarUtils.add(temp, 1, Calendar.WEEK_OF_YEAR);
            } else {
                temp = CalendarUtils.add(temp, 1, Calendar.MONTH);
            }
        }
        dates.add(max);

        // 降序
        if (sort == FormatSort.DESC) Collections.reverse(dates);

        List<String> dates2 = new ArrayList<>();
        for (Date date : dates) {
            String date2unit;
            if (calc == FormatCalc.Y || calc == FormatCalc.Q || calc == FormatCalc.W) {
                date2unit = CalendarUtils.getDateFormat("yyyy").format(date);
                Calendar cal = CalendarUtils.getInstance(date);

                if (calc == FormatCalc.Q || calc == FormatCalc.QQ) {
                    int m = cal.get(Calendar.MONTH) + 1;
                    if (m <= 3) date2unit += " Q1";
                    else if (m <= 6) date2unit += " Q2";
                    else if (m <= 9) date2unit += " Q3";
                    else date2unit += " Q4";
                } else if (calc == FormatCalc.W) {
                    int m = cal.get(Calendar.WEEK_OF_YEAR);
                    date2unit += " W" + (m < 10 ? "0" : "") + m;
                }

            } else if (calc == FormatCalc.M) {
                date2unit = CalendarUtils.getDateFormat("yyyy-MM").format(date);
            } else if (calc == FormatCalc.MM) {
                date2unit = CalendarUtils.getDateFormat("MM").format(date);
            } else {
                date2unit = CalendarUtils.getDateFormat("yyyy-MM-dd").format(date);
            }

            if (!dates2.contains(date2unit)) dates2.add(date2unit);
        }
        return dates2;
    }
}
