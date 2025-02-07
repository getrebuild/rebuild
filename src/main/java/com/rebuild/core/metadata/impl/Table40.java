/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.Dialect;
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

    public Table40(Entity entity, Dialect dialect) {
        this(entity, dialect, null);
    }

    public Table40(Entity entity, Dialect dialect, List<String> indexFields) {
        super(entity, dialect, buildIndexList(indexFields));
    }

    private static List<Element> buildIndexList(List<String> indexFields) {
        if (CollectionUtils.isEmpty(indexFields)) return null;

        List<Element> ixs = new ArrayList<>();
        for (String indexField : indexFields) {
            Element ix = XmlHelper.createDom4jElement("index");
            ix.addAttribute("field-list", indexField);
            ixs.add(ix);
        }
        return ixs;
    }

    @Override
    public void generateFieldDDL(Field field, StringBuilder into, boolean allowZeroDate) {
        StringBuilder tmp = new StringBuilder();
        super.generateFieldDDL(field, tmp, allowZeroDate);
        String tmpFix = tmp.toString().replace(" timestamp ", " datetime ");
        into.append(tmpFix);
    }
}
