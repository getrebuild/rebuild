/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.commons.ObjectUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
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

    protected TableChart(JSONObject config) {
        super(config);

        JSONObject option = config.getJSONObject("option");
        if (option != null) {
            this.showLineNumber = option.getBooleanValue("showLineNumber");
            this.showSums = option.getBooleanValue("showSums");
        }
    }

    @Override
    public JSON build() {
        Dimension[] dims = getDimensions();
        Numerical[] nums = getNumericals();

        Object[][] dataRaw = createQuery(buildSql(dims, nums)).array();

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
            for (int i = numericalIndexStart; i < colLength; i++) {
                BigDecimal sum = new BigDecimal(0);
                for (Object[] row : dataRaw) {
                    sum = sum.add(BigDecimal.valueOf(ObjectUtils.toDouble(row[i])));
                }
                sumsRow[i] = sum.doubleValue();
            }

            dataRawNew[dataRaw.length] = sumsRow;
            dataRaw = dataRawNew;
        }

        String tableHtml = new TableBuilder(this, dataRaw).toHTML();
        return JSONUtils.toJSONObject("html", tableHtml);
    }

    protected boolean isShowLineNumber() {
        return showLineNumber;
    }

    protected boolean isShowSums() {
        return showSums;
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
                getSourceEntity().getName(), getFilterSql());

        return appendSqlSort(sql);
    }
}
