/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
package com.rebuild.core.service.trigger.aviator;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Usage: LOG($param)
 * Return: None
 *
 * @author RB
 * @since 2022/11/10
 */
@Slf4j
public class LogFunction extends AbstractFunction {
    private static final long serialVersionUID = 1413021284050119794L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        log.info("\n[AVIATOR LOG] : {}", arg1.getValue(env));
        return AviatorNil.NIL;
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        log.info("\n[AVIATOR LOG] : {}, {}", arg1.getValue(env), arg2.getValue(env));
        return AviatorNil.NIL;
    }

    @Override
    public String getName() {
        return "LOG";
    }
}
