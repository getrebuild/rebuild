/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.metadata.MetadataHelper;

/**
 * 触发动作执行上下文
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/27
 */
public class ActionContext {

    final private ID sourceRecord;
    final private Entity sourceEntity;
    final private JSON actionContent;
    final private ID configId;

    /**
     * @param sourceRecord
     * @param sourceEntity
     * @param actionContent
     * @param configId
     */
    public ActionContext(ID sourceRecord, Entity sourceEntity, JSON actionContent, ID configId) {
        this.sourceRecord = sourceRecord;
        this.sourceEntity = sourceRecord != null ? MetadataHelper.getEntity(sourceRecord.getEntityCode()) : sourceEntity;
        this.actionContent = actionContent;
        this.configId = configId;
    }

    /**
     * 触发源实体
     *
     * @return
     */
    public Entity getSourceEntity() {
        return sourceEntity;
    }

    /**
     * 触发源记录
     *
     * @return
     */
    public ID getSourceRecord() {
        return sourceRecord;
    }

    /**
     * 触发内容
     *
     * @return
     */
    public JSON getActionContent() {
        return actionContent;
    }

    /**
     * 配置 ID
     *
     * @return
     */
    public ID getConfigId() {
        return configId;
    }
}
