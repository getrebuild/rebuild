/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.util.support.Table;

/**
 * Fixs timestamp to datetime
 *
 * @author Zixin (RB)
 * @since 12/28/2024
 */
public class Table40 extends Table {

    public Table40(Entity entity, Dialect dialect) {
        super(entity, dialect);
    }

    @Override
    public void generateFieldDDL(Field field, StringBuilder into, boolean allowZeroDate) {
        StringBuilder tmp = new StringBuilder();
        super.generateFieldDDL(field, tmp, allowZeroDate);
        String tmpFix = tmp.toString().replace(" timestamp ", " datetime ");
        into.append(tmpFix);
    }
}
