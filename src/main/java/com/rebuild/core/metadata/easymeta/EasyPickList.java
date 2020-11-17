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
public class EasyPickList extends EasyField {
    private static final long serialVersionUID = 5971173892145230003L;

    protected EasyPickList(Field field, DisplayType displayType) {
        super(field, displayType);
    }
}
