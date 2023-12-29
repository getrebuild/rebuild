/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorType;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;

/**
 * Wrap {@link Date}
 *
 * @author devezhao
 * @since 2021/4/12
 */
@Slf4j
public class AviatorDate extends AviatorObject {
    private static final long serialVersionUID = 2930549924386648595L;

    public static final String DU_YEAR = "Y";
    public static final String DU_MONTH = "M";
    public static final String DU_DAY = "D";
    public static final String DU_HOUR = "H";
    public static final String DU_MINUTE = "I";
    public static final String DU_SECOND = "S";

    final private Date dateValue;

    public AviatorDate(Date value) {
        super();
        this.dateValue = value;
    }

    @Override
    public int innerCompare(AviatorObject other, Map<String, Object> env) {
        Object $date = other.getValue(env);
        if ($date instanceof Date) {
            return dateValue.compareTo((Date) $date);
        }

        log.warn("Could not compare " + desc(env) + " with " + other.desc(env));
        return -1;
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
