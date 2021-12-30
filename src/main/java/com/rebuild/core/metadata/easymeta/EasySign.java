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
 * @since 2021/12/30
 */
public class EasySign extends EasyField {

    protected EasySign(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        Assert.isTrue(targetField.getDisplayType() == getDisplayType(), "type-by-type is must");
        return value;
    }
}
