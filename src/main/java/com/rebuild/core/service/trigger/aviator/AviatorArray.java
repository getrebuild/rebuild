/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorType;

import java.util.Collection;
import java.util.Map;

/**
 * Wrap {@link java.util.Collection} or Array
 *
 * @author devezhao
 * @since 2021/4/12
 */
public class AviatorArray extends AviatorObject {
    private static final long serialVersionUID = 5226071138800445209L;

    final private Object arrayValue;

    public AviatorArray(Collection value) {
        super();
        this.arrayValue = value;
    }

    public AviatorArray(Object[] value) {
        super();
        this.arrayValue = value;
    }

    @Override
    public int innerCompare(AviatorObject other, Map<String, Object> env) {
        return 0;
    }

    @Override
    public AviatorType getAviatorType() {
        return AviatorType.JavaType;
    }

    @Override
    public Object getValue(Map<String, Object> env) {
        return this.arrayValue;
    }
}
