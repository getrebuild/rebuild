/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.metadata.impl.EntityImpl;

/**
 * @author zhaofang123@gmail.com
 * @since 08/04/2018
 */
public class UnsafeEntity extends EntityImpl {
    private static final long serialVersionUID = 2107073554299141281L;

    protected UnsafeEntity(String entityName, String physicalName, String entityLabel, int typeCode, String nameFieldName) {
        super(entityName, physicalName, entityLabel, null,
                Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, typeCode, nameFieldName, Boolean.TRUE);
    }

    @Override
    protected void addField(Field field) {
        super.addField(field);
    }
}
