/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.CalendarUtils;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Usage: DATE($dateStr, [$format])
 * Return: Date
 *
 * @author RB
 * @since 2025/9/29
 */
@Slf4j
public class DateFunction extends AbstractFunction {
    private static final long serialVersionUID = -1915688036937857228L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        return call(env, arg1, AviatorNil.NIL);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        final Object $dateStr = arg1.getValue(env);
        final Object $format = arg2.getValue(env);

        Object ret;
        if ($format != null) {
            ret = CalendarUtils.parse($dateStr.toString(), $format.toString());
        } else {
            ret = CommonsUtils.parseDate($dateStr.toString());
        }

        if (ret == null) log.warn("Bad date string : {}", $dateStr);
        return AviatorUtils.wrapReturn(ret);
    }

    @Override
    public String getName() {
        return "DATE";
    }
}
