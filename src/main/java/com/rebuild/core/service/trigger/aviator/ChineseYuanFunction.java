/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.ObjectUtils;
import cn.hutool.core.convert.Convert;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorString;

import java.util.Map;

/**
 * Usage: CHINESEYUAN($number)
 * Return: String
 *
 * @author RB
 * @since 2022/10/11
 */
public class ChineseYuanFunction extends AbstractFunction {
    private static final long serialVersionUID = 4882080598493980144L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        Object o = arg1.getValue(env);
        final Number $number = o instanceof Number ? (Number) o : ObjectUtils.toDouble(o.toString());

        String cn = Convert.digitToChinese($number);
        return new AviatorString(cn);
    }

    @Override
    public String getName() {
        return "CHINESEYUAN";
    }
}
