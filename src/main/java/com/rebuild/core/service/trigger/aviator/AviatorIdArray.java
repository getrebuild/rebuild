/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.persist4j.engine.ID;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorType;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;

/**
 * Wrap {@link ID[]}
 *
 * @author devezhao
 * @since 2024/5/14
 */
@Slf4j
public class AviatorIdArray extends AviatorObject {
    private static final long serialVersionUID = 7227725706972057447L;

    final private ID[] idArray;

    public AviatorIdArray(ID[] value) {
        super();
        this.idArray = value;
    }

    public AviatorIdArray(Collection<ID> value) {
        this(value.toArray(new ID[0]));
    }

    @Override
    public int innerCompare(AviatorObject other, Map<String, Object> env) {
        log.warn("Could not compare {} with {}", desc(env), other.desc(env));
        return -1;
    }

    @Override
    public AviatorType getAviatorType() {
        return AviatorType.JavaType;
    }

    @Override
    public Object getValue(Map<String, Object> env) {
        return this.idArray;
    }
}
