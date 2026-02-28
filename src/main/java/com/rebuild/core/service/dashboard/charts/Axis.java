/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;

/**
 * 轴
 *
 * @author devezhao
 * @since 12/14/2018
 */
@Getter
public class Axis {

    private Field field;
    private FormatSort formatSort;
    private FormatCalc formatCalc;
    protected String label;
    private Field parentField;
    private String fkey;  // 唯一ID

    /**
     * @param fkey
     * @param field
     * @param sort
     * @param calc
     * @param label
     * @param parentField
     */
    protected Axis(String fkey, Field field, FormatSort sort, FormatCalc calc, String label, Field parentField) {
        this.fkey = fkey;
        this.field = field;
        this.formatSort = sort;
        this.formatCalc = calc;
        this.label = label;
        this.parentField = parentField;
    }

    // LABEL
    public String getLabel() {
        if (StringUtils.isNotBlank(label)) return label;
        return (parentField == null ? "" : (EasyMetaFactory.getLabel(parentField) + "."))
                + EasyMetaFactory.getLabel(field);
    }

    // SQLNAME
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
        return Objects.hash(field, formatCalc, parentField);
    }
}
