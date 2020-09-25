/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.impl.EasyMeta;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;

/**
 * 轴
 *
 * @author devezhao
 * @since 12/14/2018
 */
public class Axis {

    private Field field;
    private FormatSort sort;
    private FormatCalc calc;
    protected String label;
    private Field parentField;

    /**
     * @param field
     * @param sort
     * @param calc
     * @param label
     * @param parentField
     */
    protected Axis(Field field, FormatSort sort, FormatCalc calc, String label, Field parentField) {
        this.field = field;
        this.calc = calc;
        this.sort = sort;
        this.label = label;
        this.parentField = parentField;
    }

    /**
     * @return
     */
    public Field getField() {
        return field;
    }

    /**
     * @return
     */
    public FormatSort getFormatSort() {
        return sort;
    }

    /**
     * @return
     */
    public FormatCalc getFormatCalc() {
        return calc;
    }

    /**
     * @return
     */
    public String getLabel() {
        if (StringUtils.isNotBlank(label)) {
            return label;
        }
        return (parentField == null ? "" : (EasyMeta.getLabel(parentField) + ".")) + EasyMeta.getLabel(field);
    }

    /**
     * @return
     */
    public String getSqlName() {
        return (parentField == null ? "" : (parentField.getName() + ".")) + field.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return o.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, calc, parentField);
    }
}
