/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import com.rebuild.core.support.general.N2NReferenceSupport;

/**
 * @author Zixin
 * @since 2022/12/12
 * @see N2NReferenceSupport
 */
public class EasyTag extends EasyField implements MultiValue, MixValue {
    private static final long serialVersionUID = -5827184319679918289L;

    protected EasyTag(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        return super.convertCompatibleValue(value, targetField);
    }

    @Override
    public Object exprDefaultValue() {
        return super.exprDefaultValue();
    }
}
