/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.persist4j.engine.ID;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorJavaType;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorString;
import com.rebuild.core.support.general.FieldValueHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Usage: TEXT($id|$id[], [$defaultValue], [$separator])
 * Return: String
 *
 * @author RB
 * @since 2022/7/5
 */
@Slf4j
public class TextFunction extends AbstractFunction {
    private static final long serialVersionUID = 8632984920156129174L;

    private static final AviatorString BLANK = new AviatorString("");
    private static final AviatorString SEPARATOR = new AviatorString(", ");

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        return call(env, arg1, BLANK, SEPARATOR);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        return call(env, arg1, BLANK, SEPARATOR);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {
        final Object $id = arg1.getValue(env);

        // 引用 ID
        if (ID.isId($id)) {
            ID anyid = $id instanceof ID ? (ID) $id : ID.valueOf($id.toString());
            String text = FieldValueHelper.getLabel(anyid, null);
            return text == null ? arg2 : new AviatorString(text);
        }

        // 多引用 ID[]
        if ($id instanceof ID[]) {
            List<String> texts = new ArrayList<>();
            for (ID anyid : (ID[]) $id) {
                String t = FieldValueHelper.getLabel(anyid, null);
                if (t != null) texts.add(t);
            }

            if (texts.isEmpty()) return arg2;

            String sep = ObjectUtils.defaultIfNull(arg3.getValue(env), ", ").toString();
            return new AviatorString(StringUtils.join(texts, sep));
        }

        if (arg1 instanceof AviatorJavaType) {
            log.warn("Invalid value with type : {}={}", ((AviatorJavaType) arg1).getName(), $id);
        } else {
            log.warn("Invalid value with type : {}", $id);
        }

        // TODO 更多字段类型支持

        return arg2;
    }

    @Override
    public String getName() {
        return "TEXT";
    }
}
