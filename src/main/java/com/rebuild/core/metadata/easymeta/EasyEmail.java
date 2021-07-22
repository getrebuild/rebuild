/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import com.rebuild.core.support.DataDesensitized;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyEmail extends EasyField {
    private static final long serialVersionUID = -3601935952056036314L;

    protected EasyEmail(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object wrapValue(Object value) {
        Object email = super.wrapValue(value);
        return isUseDesensitized() ? DataDesensitized.email((String) email) : email;
    }
}
