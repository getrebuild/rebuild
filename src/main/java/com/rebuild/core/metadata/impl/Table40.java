/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.metadata.CascadeModel;
import cn.devezhao.persist4j.metadata.impl.FieldImpl;
import cn.devezhao.persist4j.util.XmlHelper;
import cn.devezhao.persist4j.util.support.Table;
import org.apache.commons.collections4.CollectionUtils;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixs timestamp to datetime
 *
 * @author Zixin (RB)
 * @since 12/28/2024
 */
public class Table40 extends Table {

    private boolean keepFieldKeyAttrs;

    public Table40(Entity entity, Dialect dialect) {
        this(entity, dialect, null);
    }

    public Table40(Entity entity, Dialect dialect, List<?> indexFields) {
        this(entity, dialect, buildIndexList(indexFields), false);
    }

    public Table40(Entity entity, Dialect dialect, List<?> indexFields, boolean keepFieldKeyAttrs) {
        super(entity, dialect, buildIndexList(indexFields));
        this.keepFieldKeyAttrs = keepFieldKeyAttrs;
    }

    @SuppressWarnings("unchecked")
    private static List<Element> buildIndexList(List<?> indexFields) {
        if (CollectionUtils.isEmpty(indexFields)) return null;
        if (indexFields.get(0) instanceof Element) return (List<Element>) indexFields;

        List<Element> ixs = new ArrayList<>();
        for (Object indexField : indexFields) {
            Element ix = XmlHelper.createDom4jElement("index");
            ix.addAttribute("field-list", (String) indexField);
            ixs.add(ix);
        }
        return ixs;
    }

    @Override
    public void generateFieldDDL(Field field, StringBuilder into, boolean allowZeroDate) {
        StringBuilder s = new StringBuilder();

        boolean nullable42 = keepFieldKeyAttrs ? field.isNullable() : true;
        Object defaultValue42 = keepFieldKeyAttrs ? field.getDefaultValue() : null;

        Field unsafeField = new FieldImpl(
                field.getName(), field.getPhysicalName(), field.getDescription(), null,
                true, true, true,
                field.getOwnEntity(), field.getType(), field.getMaxLength(), CascadeModel.Ignore,
                nullable42, true, field.isAutoValue(), Field2Schema.DECIMAL_SCALE, defaultValue42);
        super.generateFieldDDL(unsafeField, s, allowZeroDate);

        String fix = s.toString()
                .replace(" timestamp ", " datetime ")
                .replace(" default current_date ", " ");
        into.append(fix);
    }
}
