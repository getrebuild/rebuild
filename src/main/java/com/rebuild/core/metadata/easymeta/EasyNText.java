/*!
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
public class EasyNText extends EasyField {
    private static final long serialVersionUID = -3986451731566643240L;

    protected EasyNText(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object exprDefaultValue() {
        Object d = super.exprDefaultValue();
        if (d != null) d = d.toString().replace("\\n", "\n");  // 换行符
        return d;
    }
}
