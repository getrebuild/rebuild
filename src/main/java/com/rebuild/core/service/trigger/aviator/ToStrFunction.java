/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.CalendarUtils;
import com.alibaba.fastjson.JSON;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

/**
 * Usage: TOSTR($object)
 * Return: String
 *
 * @author RB
 * @since 2025/9/30
 */
@Slf4j
public class ToStrFunction extends AbstractFunction {
    private static final long serialVersionUID = 5019787793358720076L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        final Object $object = arg1.getValue(env);

        if ($object == null) return AviatorNil.NIL;
        if ($object instanceof String) AviatorUtils.wrapReturn($object);

        String str;
        if ($object instanceof Date) str = CalendarUtils.getUTCDateTimeFormat().format($object);
        else if ($object instanceof LocalDateTime) str = CalendarUtils.getDateFormat("HH:mm:ss").format($object);
        else if ($object instanceof Iterable) str = JSON.toJSONString($object);
        else str = $object.toString();

        return AviatorUtils.wrapReturn(str);
    }

    @Override
    public String getName() {
        return "TOSTR";
    }
}
