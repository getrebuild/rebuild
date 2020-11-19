/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.support.general.BarCodeGenerator;
import org.springframework.util.Assert;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyBarCode extends EasyField {
    private static final long serialVersionUID = 5175455130040618922L;

    protected EasyBarCode(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        Assert.isTrue(targetField.getDisplayType() == getDisplayType(), "type-by-type is must");
        return value;
    }

    @Override
    public Object wrapValue(Object value) {
        if (value instanceof ID) {
            return BarCodeGenerator.getBarCodeContent(getRawMeta(), (ID) value);
        }
        return null;
    }
}
