/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorJavaType;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorString;
import com.googlecode.aviator.runtime.type.Sequence;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Usage: TEXT($id|$id[], [$defaultValue], [$separator], [$labelFieldName])
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
        return call(env, arg1, BLANK, SEPARATOR, AviatorNil.NIL);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        return call(env, arg1, arg2, SEPARATOR, AviatorNil.NIL);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {
        return call(env, arg1, arg2, arg3, AviatorNil.NIL);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3, AviatorObject arg4) {
        final Object $id = arg1.getValue(env);
        final Object $defaultValue = arg2.getValue(env);
        final String sep = ObjectUtils.defaultIfNull(arg3.getValue(env), ", ").toString();
        final String fieldName = arg4.getValue(env) == null ? null : arg4.getValue(env).toString();

        // No value
        if ($id == null) return arg2;

        // 引用 ID
        if (ID.isId($id)) {
            ID anyid = $id instanceof ID ? (ID) $id : ID.valueOf($id.toString());
            String text = getLabel(anyid, fieldName);

            if (text == null && $defaultValue != null) text = $defaultValue.toString();
            return new AviatorString(text);
        }

        // 多引用 ID[]

        Object idArray = $id;
        if (idArray instanceof Collection || idArray instanceof Sequence) {
            Iterator<?> iter = AviatorUtils.toIterator(idArray);
            List<ID> list = new ArrayList<>();
            while (iter.hasNext()) {
                Object o = iter.next();
                if (o instanceof ID) list.add((ID) o);
            }
            if (!list.isEmpty()) idArray = list.toArray(new ID[0]);
        }

        if (idArray instanceof ID[]) {
            List<String> text = new ArrayList<>();
            for (ID anyid : (ID[]) idArray) {
                String item = getLabel(anyid, fieldName);

                if (item == null && $defaultValue != null) item = $defaultValue.toString();
                if (item != null) text.add(item);
            }

            if (text.isEmpty()) return arg2;

            return new AviatorString(StringUtils.join(text, sep));
        }

        if (arg1 instanceof AviatorJavaType) {
            log.warn("Invalid value with type : {}={}", ((AviatorJavaType) arg1).getName(), $id);
        } else {
            log.warn("Invalid value with type : {}", $id);
        }

        // TODO 更多字段类型支持

        return arg2;
    }

    // 获取字段内容
    private String getLabel(ID id, String fieldName) {
        if (fieldName == null) return FieldValueHelper.getLabelNotry(id);

        Entity entity = MetadataHelper.getEntity(id.getEntityCode());
        Field field = MetadataHelper.getLastJoinField(entity, fieldName);

        Object[] o = Application.getQueryFactory().uniqueNoFilter(id, fieldName);
        if (o == null || o[0] == null) return null;

        Object label = FieldValueHelper.wrapFieldValue(o[0], field, true);
        return label == null ? null : label.toString();
    }

    @Override
    public String getName() {
        return "TEXT";
    }
}
