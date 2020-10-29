/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.transform;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * TODO
 *
 * @author devezhao
 * @since 2020/10/27
 */
public class RecordTransfomer {

    private Entity targetEntity;
    private JSONObject transConfig;

    /**
     * @param targetEntity
     */
    public RecordTransfomer(Entity targetEntity, JSONObject transConfig) {
        this.targetEntity = targetEntity;
    }

    /**
     * @return
     */
    public boolean checkFilter() {
        return false;
    }

    /**
     * @param sourceRecord
     * @return
     */
    public ID transform(ID sourceRecord) {
        return null;
    }
}
