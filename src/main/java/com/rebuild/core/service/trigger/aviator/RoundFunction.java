/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.ObjectUtils;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Usage: ROUND($number, $scale, [$mode])
 * Return: NUMBER
 *
 * @author devezhao
 * @since 2024/8/27
 */
public class RoundFunction extends AbstractFunction {
    private static final long serialVersionUID = -6731627245536290306L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {
        final Object $number = arg1.getValue(env);
        final Object $scale = arg2.getValue(env);
        final Object $mode = arg3.getValue(env);

        double number = ObjectUtils.toDouble($number);
        int scale = ObjectUtils.toInt($scale, 2);
        int mode = ObjectUtils.toInt($mode, 0);

        RoundingMode roundingMode = RoundingMode.HALF_UP;
        if (mode == 1) roundingMode = RoundingMode.CEILING;  // 向上位
        else if (mode == 2) roundingMode = RoundingMode.DOWN;  // 向下位

        double newVal = BigDecimal.valueOf(number).setScale(scale, roundingMode).doubleValue();
        return AviatorUtils.wrapReturn(newVal);
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
        return "ROUND";
    }
}
