/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import org.springframework.util.Assert;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyLocation extends EasyField {
    private static final long serialVersionUID = -3380324396602087075L;

    protected EasyLocation(Field field, DisplayType displayType) {
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
