/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.CalendarUtils;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorObject;

import java.util.Map;

/**
 * Usage: CURRENTDATE()
 * Return: Date
 *
 * @author devezhao
 * @since 2022/2/25
 */
public class CurrentDateFunction extends AbstractFunction {
    private static final long serialVersionUID = -6731627245536290306L;

    @Override
    public AviatorObject call(Map<String, Object> env) {
        return new AviatorDate(CalendarUtils.now());
    }

    @Override
    public String getName() {
        return "CURRENTDATE";
    }
}
