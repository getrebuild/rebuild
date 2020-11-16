/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easy;

import cn.devezhao.persist4j.Field;

/**
 * TODO
 *
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyField extends BaseEasyMeta<Field> {
    private static final long serialVersionUID = 6027165766338449527L;

    protected EasyField(Field field) {
        super(field);
    }
}
