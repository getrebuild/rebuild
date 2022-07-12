/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/


package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.persist4j.engine.ID;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorString;
import com.rebuild.core.support.general.FieldValueHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Usage: TEXT($id, [$defaultValue])
 * Return: String
 *
 * @author RB
 * @since 2022/7/5
 */
@Slf4j
public class TextFunction extends AbstractFunction {
    private static final long serialVersionUID = 8632984920156129174L;

    private static final AviatorString BLANK = new AviatorString("");

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        return call(env, arg1, BLANK);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        Object o = arg1.getValue(env);
        if (!ID.isId(o)) return arg2;

        ID anyid = o instanceof ID ? (ID) o : ID.valueOf(o.toString());
        String text = FieldValueHelper.getLabel(anyid, null);

        return text == null ? arg2 : new AviatorString(text);
    }

    @Override
    public String getName() {
        return "TEXT";
    }
}
