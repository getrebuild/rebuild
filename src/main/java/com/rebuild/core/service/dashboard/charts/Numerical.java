/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.DisplayType;
import org.apache.commons.lang.StringUtils;

/**
 * 数值-轴
 *
 * @author devezhao
 * @since 12/14/2018
 */
public class Numerical extends Axis {

    private JSONObject filter;
    private int scale = 2;

    /**
     * @param field
     * @param sort
     * @param calc
     * @param label
     * @param scale
     * @param filter
     * @param parentField
     */
    protected Numerical(Field field, FormatSort sort, FormatCalc calc, String label, Integer scale,
                        JSONObject filter, Field parentField) {
        super(field, sort, calc, label, parentField);
        if (scale != null) this.scale = scale;
        this.filter = filter;
    }

    /**
     * 小数位
     * @return
     */
    public int getScale() {
        return scale;
    }

    /**
     * 字段筛选条件
     * @return
     */
    public JSONObject getFilter() {
        return filter;
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
            return String.format("%s(%s)", getFormatCalc().name(), super.getSqlName());
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
