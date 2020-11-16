/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easy;

import cn.devezhao.persist4j.metadata.BaseMeta;
import com.alibaba.fastjson.JSONObject;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public abstract class BaseEasyMeta<T extends BaseMeta> implements BaseMeta {
    private static final long serialVersionUID = 6088391941883022085L;

    private final T baseMeta;

    protected BaseEasyMeta(T baseMeta) {
        this.baseMeta = baseMeta;
    }

    @Override
    public String getName() {
        return baseMeta.getName();
    }

    @Override
    public String getPhysicalName() {
        return baseMeta.getPhysicalName();
    }

    @Override
    public String getDescription() {
        return baseMeta.getDescription();
    }

    @Override
    public boolean isCreatable() {
        return baseMeta.isCreatable();
    }

    @Override
    public boolean isUpdatable() {
        return baseMeta.isUpdatable();
    }

    @Override
    public boolean isQueryable() {
        return baseMeta.isQueryable();
    }

    @Override
    public JSONObject getExtraAttrs() {
        return baseMeta.getExtraAttrs();
    }

    /**
     * @return
     */
    public T getRawMeta() {
        return baseMeta;
    }
}
