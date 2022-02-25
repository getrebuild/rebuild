/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorType;

import java.util.Date;
import java.util.Map;

/**
 * @author devezhao
 * @since 2021/4/12
 */
public class AviatorDate extends AviatorObject {
    private static final long serialVersionUID = 2930549924386648595L;

    protected static final String DU_HOUR = "H";
    protected static final String DU_DAY = "D";
    protected static final String DU_MONTH = "M";
    protected static final String DU_YEAR = "Y";

    final private Date dateValue;

    protected AviatorDate(Date value) {
        super();
        this.dateValue = value;
    }

    @Override
    public int innerCompare(AviatorObject other, Map<String, Object> env) {
        return 0;
    }

    @Override
    public AviatorType getAviatorType() {
        return AviatorType.JavaType;
    }

    @Override
    public Object getValue(Map<String, Object> env) {
        return this.dateValue;
    }
}
