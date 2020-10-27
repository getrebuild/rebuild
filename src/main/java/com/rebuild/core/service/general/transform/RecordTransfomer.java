/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.transform;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;

/**
 * TODO
 *
 * @author devezhao
 * @since 2020/10/27
 */
public class RecordTransfomer {

    private Entity targetEntity;

    /**
     * @param targetEntity
     */
    public RecordTransfomer(Entity targetEntity) {
        this.targetEntity = targetEntity;
    }

    /**
     * @param sourceRecord
     * @param useMapping
     * @return
     */
    public ID transform(ID sourceRecord, JSON useMapping) {
        return null;
    }
}
