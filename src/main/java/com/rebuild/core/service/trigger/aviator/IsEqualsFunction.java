/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Usage: ISEQUALS($a, $b)
 * Return: Boolean
 *
 * @author RB
 * @since 2026/4/21
 */
@Slf4j
public class IsEqualsFunction extends AbstractFunction {
    private static final long serialVersionUID = -2743278680098427545L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        final Object val1 = arg1.getValue(env);
        final Object val2 = arg2.getValue(env);
        return AviatorUtils.wrapReturn(CommonsUtils.isSame(val1, val2));
    }

    @Override
    public String getName() {
        return "ISEQUALS";
    }
}
