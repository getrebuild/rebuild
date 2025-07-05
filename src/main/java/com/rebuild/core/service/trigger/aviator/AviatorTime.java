/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorType;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.util.Date;
import java.util.Map;

/**
 * Wrap {@link LocalTime}
 *
 * @author devezhao
 * @since 2021/4/12
 */
@Slf4j
public class AviatorTime extends AviatorObject {
    private static final long serialVersionUID = 2930549924386648595L;

    final private LocalTime timeValue;

    public AviatorTime(LocalTime value) {
        super();
        this.timeValue = value;
    }

    @Override
    public int innerCompare(AviatorObject other, Map<String, Object> env) {
        Object $time = other.getValue(env);
        if ($time instanceof LocalTime) {
            return timeValue.compareTo((LocalTime) $time);
        }

        log.warn("Could not compare {} with {}", desc(env), other.desc(env));
        return -1;
    }

    @Override
    public AviatorType getAviatorType() {
        return AviatorType.JavaType;
    }

    @Override
    public Object getValue(Map<String, Object> env) {
        return this.timeValue;
    }
}
