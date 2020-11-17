/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyAnyReference extends EasyField {
    private static final long serialVersionUID = -5775035002469908191L;

    protected EasyAnyReference(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object wrapValue(Object value) {
        throw new UnsupportedOperationException();
    }
}
