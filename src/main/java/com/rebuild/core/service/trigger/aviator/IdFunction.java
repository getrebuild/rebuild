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
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.service.query.QueryHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.NumberUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Usage: ID($text, $entity)
 * Return: ID
 *
 * @author RB
 * @since 2023/4/15
 */
@Slf4j
public class IdFunction extends AbstractFunction {
    private static final long serialVersionUID = 6502942566729742491L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        final Object $text = arg1.getValue(env);
        final Object $entity = arg2.getValue(env);

        Entity entity;
        if (NumberUtils.isNumber($entity.toString())) {
            entity = MetadataHelper.getEntity(NumberUtils.toInt($entity.toString()));
        } else {
            entity = MetadataHelper.getEntity($entity.toString());
        }

        // 查找名称字段和自动编号字段
        Set<Field> queryFields = new HashSet<>();
        queryFields.add(entity.getNameField());
        queryFields.addAll(Arrays.asList(MetadataSorter.sortFields(entity, DisplayType.SERIES)));

        ID found = QueryHelper.queryId(queryFields.toArray(new Field[0]), $text.toString());
        return found == null ? AviatorNil.NIL : new AviatorId(found);
    }

    @Override
    public String getName() {
        return "ID";
    }
}
