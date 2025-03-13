/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.hutool.core.date.ChineseDate;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorBoolean;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorString;

import java.util.Date;
import java.util.Map;

/**
 * Usage: CHINESEDATE($date, [$traditional])
 * Return: Date
 *
 * @author devezhao
 * @since 2025/3/10
 */
public class ChineseDateFunction extends AbstractFunction {
    private static final long serialVersionUID = 8286269123896553078L;

    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        return call(env, arg1, AviatorBoolean.FALSE);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        Object o = arg1.getValue(env);
        final Date $date = o instanceof Date ? (Date) o : CalendarUtils.parse(o.toString());
        if ($date == null) return AviatorNil.NIL;

        // 是否农历
        boolean $traditional = ObjectUtils.toBool(arg2.getValue(env));

        String dstr;
        if ($traditional) {
            ChineseDate cd = new ChineseDate($date);
            dstr = String.format("%s%s %s年(%d)",
                    cd.getChineseMonth(true), cd.getChineseDay(), cd.getCyclical(), cd.getChineseYear());
        } else {
            dstr = CalendarUtils.getCNDateFormat().format($date);
        }
        return new AviatorString(dstr);
    }

    @Override
    public String getName() {
        return "CHINESEDATE";
    }
}
