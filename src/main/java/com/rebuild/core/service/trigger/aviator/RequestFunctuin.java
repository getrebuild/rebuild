/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorString;
import com.rebuild.utils.HttpUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

/**
 * Usage: REQUEST(url, [defaultValue])
 * Return: String
 *
 * @author devezhao
 * @since 2022/3/2
 */
@Slf4j
public class RequestFunctuin extends AbstractFunction {
    private static final long serialVersionUID = -731061967737775464L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        return call(env, arg1, AviatorNil.NIL);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        String requestUrl = arg1.getValue(env).toString();

        String res = null;
        try {
            res = HttpUtils.get(requestUrl);
        } catch (IOException ex) {
            log.error("Request fail : {}", requestUrl, ex);
        }

        return res == null ? arg2 : new AviatorString(res);
    }

    @Override
    public String getName() {
        return "REQUEST";
    }
}
