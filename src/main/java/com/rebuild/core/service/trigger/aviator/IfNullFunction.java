/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Usage: IFNULL($any, $default)
 * Return: Boolean
 *
 * @author RB
 * @since 2025/12/22
 */
@Slf4j
public class IfNullFunction extends AbstractFunction {
    private static final long serialVersionUID = -7849948179882904490L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        Object $any = arg1.getValue(env);
        Object $default = arg2.getValue(env);

        boolean is = IsNullFunction.isNull($any);
        return AviatorUtils.wrapReturn(is ? $any : $default);
    }

    @Override
    public String getName() {
        return "IFNULL";
    }
}
