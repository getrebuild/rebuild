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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
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

        // 引用
        if (ID.isId(o)) {
            ID anyid = o instanceof ID ? (ID) o : ID.valueOf(o.toString());
            String text = FieldValueHelper.getLabel(anyid, null);
            return text == null ? arg2 : new AviatorString(text);
        }

        // 多引用
        if (o instanceof ID[]) {
            List<String> texts = new ArrayList<>();
            for (ID anyid : (ID[]) o) {
                String t = FieldValueHelper.getLabel(anyid, null);
                if (t != null) texts.add(t);
            }
            return texts.isEmpty() ? arg2 : new AviatorString(StringUtils.join(texts, ", "));
        }

        if (arg1 instanceof AviatorJavaType) {
            log.warn("Invalid value with type : {}={}", ((AviatorJavaType) arg1).getName(), o);
        } else {
            log.warn("Invalid value with type : {}", o);
        }

        // TODO 更多字段类型支持

        return arg2;
    }

    @Override
    public String getName() {
        return "TEXT";
    }
}
