/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.CalendarUtils;
import com.alibaba.fastjson.JSON;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.utils.Env;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
        Object $object = arg1.getValue(env);
        String str = toStringValue($object);
        return AviatorUtils.wrapReturn(str);
    }

    /**
     * @param o
     * @return
     */
    private String toStringValue(Object o) {
        if (o instanceof Date) {
            return CalendarUtils.getUTCDateTimeFormat().format(o);
        } else if (o instanceof LocalDateTime) {
            return CalendarUtils.getDateFormat("HH:mm:ss").format(o);
        } else if (o instanceof Iterable) {
            List<String> s = new ArrayList<>();
            for (Iterator<?> iter = AviatorUtils.toIterator(o); iter.hasNext(); ) {
                Object item = iter.next();
                s.add(toStringValue(item));
            }
            return JSON.toJSONString(s);
        } else if (o instanceof AviatorObject) {
            o = ((AviatorObject) o).getValue(Env.EMPTY_ENV);
            if (o == null) return null;
            return toStringValue(o);
        }
        return AviatorUtils.toStringValue(o);
    }

    @Override
    public String getName() {
        return "TOSTR";
    }
}
