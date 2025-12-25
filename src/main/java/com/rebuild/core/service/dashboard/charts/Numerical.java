/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.query.ParseHelper;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

/**
 * 数值-轴
 *
 * @author devezhao
 * @since 12/14/2018
 */
public class Numerical extends Axis {

    @Getter
    private JSONObject filter = null;
    @Getter
    private int scale = 2;
    @Getter
    private int unit = 0;
    @Getter
    private String formatFormula;

    /**
     * @param fkey
     * @param field
     * @param sort
     * @param calc
     * @param label
     * @param scale
     * @param unit
     * @param formula
     * @param filter
     * @param parentField
     */
    protected Numerical(String fkey, Field field, FormatSort sort, FormatCalc calc, String label, Integer scale, Integer unit,
                        String formula, JSONObject filter, Field parentField) {
        super(fkey, field, sort, calc, label, parentField);
        if (scale != null) this.scale = scale;
        if (unit != null) this.unit = unit;
        if (ParseHelper.validAdvFilter(filter)) this.filter = filter;
        if (calc == FormatCalc.FORMULA) this.formatFormula = formula;
    }

    @Override
    public String getLabel() {
        if (FormatCalc.NONE == getFormatCalc()) {
            return StringUtils.defaultIfBlank(label, "数值");
        }
        return super.getLabel() + (StringUtils.isBlank(label) ? getFormatCalc().getLabel() : "");
    }

    @Override
    public String getSqlName() {
        if (FormatCalc.NONE == getFormatCalc()) {
            return super.getSqlName();
        }

        DisplayType dt = EasyMetaFactory.getDisplayType(getField());
        if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
            if (getFormatCalc() == FormatCalc.FORMULA) {
                return String.format("SUM(%s)", super.getSqlName());
            } else {
                return String.format("%s(%s)", getFormatCalc().name(), super.getSqlName());
            }
        } else if (getFormatCalc() == FormatCalc.COUNT2) {
            return String.format("%s(DISTINCT %s)", FormatCalc.COUNT, super.getSqlName());
        } else {
            return String.format("%s(%s)", FormatCalc.COUNT, super.getSqlName());
        }
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
