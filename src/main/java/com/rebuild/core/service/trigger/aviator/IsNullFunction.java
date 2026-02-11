/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.NullValue;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorBoolean;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;

/**
 * Usage: ISNULL($any)
 * Return: Boolean
 *
 * @author RB
 * @since 2023/10/18
 */
@Slf4j
public class IsNullFunction extends AbstractFunction {
    private static final long serialVersionUID = -7849948179882904490L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        boolean is = isNull(arg1.getValue(env));
        return AviatorBoolean.valueOf(is);
    }

    /**
     * 是否空 https://getrebuild.com/docs/topic/write-formula#ISNULL
     * 
     * @param $any
     * @return
     */
    protected static boolean isNull(Object $any) {
        if (NullValue.isNull($any) || $any == AviatorNil.NIL) {
            return true;
        } else if ($any instanceof Number) {
            if (ObjectUtils.toDouble($any) == 0d) return true;
        } else if ($any instanceof Object[]) {
            return ((Object[]) $any).length == 0;
        } else if ($any instanceof Collection) {
            return ((Collection<?>) $any).isEmpty();
        } else if ($any instanceof Map) {
            return ((Map<?, ?>) $any).isEmpty();
        } else if ($any instanceof Iterable) {
            return !((Iterable<?>) $any).iterator().hasNext();
        }

        return StringUtils.isEmpty($any.toString());
    }

    @Override
    public String getName() {
        return "ISNULL";
    }
}
