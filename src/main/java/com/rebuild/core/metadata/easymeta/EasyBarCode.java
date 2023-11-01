/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.support.general.BarCodeSupport;
import lombok.extern.slf4j.Slf4j;

/**
 * @author devezhao
 * @since 2020/11/17
 * @see BarCodeSupport
 */
@Slf4j
public class EasyBarCode extends EasyField {
    private static final long serialVersionUID = 5175455130040618922L;

    protected EasyBarCode(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object wrapValue(Object value) {
        // value 是记录主键
        if (value instanceof ID) {
            return BarCodeSupport.getBarCodeContent(getRawMeta(), (ID) value);
        }

        if (value != null) log.warn("Cannot wrap value of EasyBarCode : " + value);
        return null;
    }

    @Override
    public Object exprDefaultValue() {
        return null;
    }
}
