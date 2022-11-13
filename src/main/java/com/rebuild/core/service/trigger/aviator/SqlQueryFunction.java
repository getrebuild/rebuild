/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorDecimal;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorString;
import com.rebuild.core.Application;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;

/**
 * Usage: SQLQUERY($sql, [ $param...{5} ])
 * Return: Number|Date|String
 *
 * @author RB
 * @since 2022/11/10
 */
@Slf4j
public class SqlQueryFunction extends AbstractFunction {
    private static final long serialVersionUID = 6408510455471045309L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3, AviatorObject arg4, AviatorObject arg5, AviatorObject arg6) {
        String $sql = arg1.getValue(env).toString();
        Query query = Application.createQueryNoFilter($sql);

        Object param1 = getValue(env, arg2);
        if (param1 != null) query.setParameter(1, param1);
        Object param2 = getValue(env, arg3);
        if (param2 != null) query.setParameter(2, param2);
        Object param3 = getValue(env, arg4);
        if (param3 != null) query.setParameter(3, param3);
        Object param4 = getValue(env, arg5);
        if (param4 != null) query.setParameter(4, param4);
        Object param5 = getValue(env, arg6);
        if (param5 != null) query.setParameter(5, param5);

        Object[] o = query.unique();
        if (o == null || o[0] == null) return AviatorNil.NIL;

        final Object value = o[0];

        if (value instanceof Number) {
            return new AviatorDecimal((Number) value);
        } else if (value instanceof Date) {
            return new AviatorDate((Date) value);
        } else {
            return new AviatorString(value.toString());
        }
    }

    private Object getValue(Map<String, Object> env, AviatorObject arg) {
        Object value = arg.getValue(env);
        if (value == null) return null;

        if (value instanceof Number || value instanceof Date || value instanceof ID) return value;
        else return value.toString();
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3, AviatorObject arg4, AviatorObject arg5) {
        return this.call(env, arg1, arg2, arg3, arg4, arg5, AviatorNil.NIL);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3, AviatorObject arg4) {
        return this.call(env, arg1, arg2, arg3, arg4, AviatorNil.NIL);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {
        return this.call(env, arg1, arg2, arg3, AviatorNil.NIL);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        return this.call(env, arg1, arg2, AviatorNil.NIL);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        return this.call(env, arg1, AviatorNil.NIL);
    }

    @Override
    public String getName() {
        return "SQLQUERY";
    }
}
