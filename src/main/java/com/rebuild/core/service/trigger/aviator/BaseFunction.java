/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorObject;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * @author devezhao
 * @since 2026/5/21
 */
public abstract class BaseFunction extends AbstractFunction {
    private static final long serialVersionUID = -7173381979437148372L;

    public String getString(Map<String, Object> env, AviatorObject arg) {
        Object v = arg.getValue(env);
        return v == null || StringUtils.isBlank(v.toString()) ? null : v.toString();
    }

    public String getStringNotNull(Map<String, Object> env, AviatorObject arg) {
        String v = getString(env, arg);
        if (v == null) throw new FunctionException("Blank Arg : " + arg);
        return v;
    }

    public String getString(Map<String, Object> env, AviatorObject arg, String defaultValue) {
        String v = getString(env, arg);
        if (v == null) return defaultValue;
        return v;
    }
}
