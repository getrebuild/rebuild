/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.persist4j.engine.ID;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorType;

import java.util.Map;

/**
 * @author devezhao
 * @since 2022/2/25
 */
public class AviatorID extends AviatorObject  {
    private static final long serialVersionUID = 3918766843118319861L;

    final private ID idValue;

    protected AviatorID(ID id) {
        super();
        this.idValue = id;
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
        return idValue;
    }
}
