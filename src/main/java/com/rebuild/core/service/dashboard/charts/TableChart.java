/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 表格
 *
 * @author devezhao
 * @since 12/15/2018
 */
public class TableChart extends ChartData {

    private boolean showLineNumber = false;
    private boolean showSums = false;
    private boolean mergeCell = true;
    private int pageSize = 0;

    protected TableChart(JSONObject config) {
        super(config);

        JSONObject option = config.getJSONObject("option");
        if (option != null) {
            this.showLineNumber = option.getBooleanValue("showLineNumber");
            this.showSums = option.getBooleanValue("showSums");
            if (option.containsKey("mergeCell")) this.mergeCell = option.getBooleanValue("mergeCell");
            if (option.containsKey("pageSize")) this.pageSize = option.getIntValue("pageSize");
        }
    }

    @Override
    public JSON build() {
        Dimension[] dims = getDimensions();
        Numerical[] nums = getNumericals();

        Object[][] dataRaw;
        // 需要分别查询
        if (nums.length > 1 && hasNumericalFilter(nums)) {
            List<AxisEntry> axisValues = new ArrayList<>();
            int indexAndSize = 0;
            for (Numerical num : nums) {
                Object[][] array = createQuery(buildSql(dims, new Numerical[]{num})).array();
                for (Object[] o : array) axisValues.add(new AxisEntry(o, indexAndSize));
                indexAndSize++;
            }

            // TODO Table 支持 `useComparison`
            dataRaw = mergeAxisEntry2Data(axisValues, indexAndSize, false);
        } else {
            dataRaw = createQuery(buildSql(dims, nums)).array();
        }

        // 计算字段
        this.calcFormula43(dataRaw, nums);

        // v3.9
        if (pageSize > 0 && dataRaw.length > pageSize) {
            dataRaw = ArrayUtils.subarray(dataRaw, 0, pageSize);
        }

        // 行号
        if (this.showLineNumber && dataRaw.length > 0) {
            for (int i = 0; i < dataRaw.length; i++) {
                Object[] row = dataRaw[i];
                Object[] rowLN = new Object[row.length + 1];
                System.arraycopy(row, 0, rowLN, 1, row.length);
                rowLN[0] = i + 1;
                dataRaw[i] = rowLN;
            }
        }

        // 汇总
        if (this.showSums && dataRaw.length > 0) {
            Object[][] dataRawNew = new Object[dataRaw.length + 1][];
            System.arraycopy(dataRaw, 0, dataRawNew, 0, dataRaw.length);

            int colLength = dataRaw[0].length;
            Object[] sumsRow = new Object[colLength];
            int numericalIndexStart = dims.length + (this.showLineNumber ? 1 : 0);
            for (int i = 0; i < numericalIndexStart; i++) {
                if (i == 0 && this.showLineNumber) {
                    sumsRow[i] = StringUtils.EMPTY;
                } else {
                    sumsRow[i] = dataRaw.length;
                }
            }

            for (int i = numericalIndexStart, j = 0; i < colLength; i++, j++) {
                final Numerical N = getNumericals()[j];
                final FormatCalc FC = N.getFormatCalc();

                BigDecimal sums = null;
                if (FC == FormatCalc.MAX) {
                    // 最大
                    for (Object[] row : dataRaw) {
                        BigDecimal b = BigDecimal.valueOf(ObjectUtils.toDouble(row[i]));
                        if (sums == null || sums.compareTo(b) < 0) sums = b;
                    }
                } else if (FC == FormatCalc.MIN) {
                    // 最小
                    for (Object[] row : dataRaw) {
                        BigDecimal b = BigDecimal.valueOf(ObjectUtils.toDouble(row[i]));
                        if (sums == null || sums.compareTo(b) > 0) sums = b;
                    }
                } else {
                    sums = new BigDecimal(0);
                    // 求和
                    for (Object[] row : dataRaw) {
                        sums = sums.add(BigDecimal.valueOf(ObjectUtils.toDouble(row[i])));
                    }
                    // 均值
                    if (FC == FormatCalc.AVG) {
                        sums = sums.divide(new BigDecimal(dataRaw.length), N.getScale(), RoundingMode.HALF_UP);
                    }
                }
                sumsRow[i] = sums.doubleValue();
            }

            dataRawNew[dataRaw.length] = sumsRow;
            dataRaw = dataRawNew;
        }

        String tableHtml = new TableBuilder(this, dataRaw).toHTML();

        JSONObject renderOption = config.getJSONObject("option");
        if (renderOption == null) renderOption = new JSONObject();

        return JSONUtils.toJSONObject(
                new String[]{"html", "_renderOption"},
                new Object[]{tableHtml, renderOption});
    }

    protected boolean isShowLineNumber() {
        return showLineNumber;
    }

    protected boolean isShowSums() {
        return showSums;
    }

    protected boolean isMergeCell() {
        return mergeCell;
    }

    protected String wrapSumValue(Axis sumAxis, Object value) {
        if (ChartsHelper.isZero(value)) {
            return ChartsHelper.VALUE_ZERO;
        }

        if (sumAxis instanceof Numerical) {
            return wrapAxisValue((Numerical) sumAxis, value, true);
        } else {
            return value.toString();
        }
    }

    private String buildSql(Dimension[] dims, Numerical[] nums) {
        List<String> dimSqlItems = new ArrayList<>();
        for (Dimension dim : dims) {
            dimSqlItems.add(dim.getSqlName());
        }
        List<String> numSqlItems = new ArrayList<>();
        for (Numerical num : nums) {
            numSqlItems.add(num.getSqlName());
        }

        String sql = "select {0},{1} from {2} where {3} group by {0}";
        if (dimSqlItems.isEmpty()) {
            sql = "select {1} from {2} where {3}";
        } else if (numSqlItems.isEmpty()) {
            sql = "select {0} from {2} where {3} group by {0}";
        }

        sql = MessageFormat.format(sql,
                StringUtils.join(dimSqlItems, ", "),
                StringUtils.join(numSqlItems, ", "),
                getSourceEntity().getName(), getFilterSql(nums.length > 0 ? nums[0] : null));

        return appendSqlSort(sql);
    }

    @Override
    protected String wrapAxisValue(Numerical numerical, Object value, boolean useThousands) {
        if (ChartsHelper.isZero(value)) return ChartsHelper.VALUE_ZERO;
        if (ID.isId(value)) value = 1d;

        String flag = getNumericalFlag(numerical);
        if (StringUtils.isBlank(flag) || "0:0".equals(flag)) return super.wrapAxisValue(numerical, value, useThousands);

        // v3.9
        String flagUnit = "";
        int unit = Integer.parseInt(flag.split(":")[1]);
        if (unit > 0) {
            double d = ObjectUtils.toDouble(value);
            switch (unit) {
                case 1000: {
                    d /= 1000;
                    flagUnit = Language.L("千");
                    break;
                }
                case 10000: {
                    d /= 10000;
                    flagUnit = Language.L("万");
                    break;
                }
                case 100000: {
                    d /= 100000;
                    flagUnit = Language.L("十万");
                    break;
                }
                case 1000000: {
                    d /= 1000000;
                    flagUnit = Language.L("百万");
                    break;
                }
                case 10000000: {
                    d /= 10000000;
                    flagUnit = Language.L("千万");
                    break;
                }
                case 100000000: {
                    d /= 100000000;
                    flagUnit = Language.L("亿");
                    break;
                }
            }
            value = d;
        }

        String n = super.wrapAxisValue(numerical, value, useThousands);
        return n + flagUnit;
    }
}
