/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.MetadataHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 批量操作上下文
 *
 * @author devezhao
 * @since 10/17/2018
 */
public class BulkContext {

    // 操作用户
    private ID opUser;
    // 执行动作
    private Permission action;
    // [目标用户]
    private ID toUser;

    // 待操作记录
    private ID[] records;
    // [待操作记录所依附的主记录]
    private ID targetRecord;
    // [级联操作实体]
    private String[] cascades;

    // 扩展数据
    // customData = [特定数据] 默认为高级查询表达式，如果为查询条件，其必须含有查询项，否则将抛出异常
    // shareRights = 共享用，指定权限值
    private Map<String, Object> extraParams = new HashMap<>();

    final private Entity mainEntity;

    /**
     * @param opUser 操作用户
     * @param action 动作
     * @param toUser 目标用户
     * @param cascades 级联实体
     * @param records 操作记录
     * @param recordMain 主记录
     * @param extraParams
     */
    BulkContext(ID opUser, Permission action, ID toUser, String[] cascades, ID[] records, ID recordMain, Map<String, Object> extraParams) {
        this.opUser = opUser;
        this.action = action;
        this.toUser = toUser;
        this.records = records;
        this.targetRecord = recordMain;
        this.cascades = cascades;
        if (extraParams != null) this.extraParams.putAll(extraParams);
        this.mainEntity = detecteMainEntity();
    }

    /**
     * 有目标用户的，如分派/共享/删除
     *
     * @param opUser
     * @param action
     * @param toUser
     * @param cascades
     * @param records
     */
    public BulkContext(ID opUser, Permission action, ID toUser, String[] cascades, ID[] records) {
        this(opUser, action, toUser, cascades, records, null, null);
    }

    /**
     * 无目标用户的，如取消共享
     *
     * @param opUser
     * @param action
     * @param records
     * @param targetRecord
     */
    public BulkContext(ID opUser, Permission action, ID[] records, ID targetRecord) {
        this(opUser, action, null, null, records, targetRecord, null);
    }

    /**
     * 一般批量
     *
     * @param opUser
     * @param action
     * @param customData
     */
    public BulkContext(ID opUser, Permission action, JSONObject customData) {
        this(opUser, action, null, null, null, null,
                Collections.singletonMap("customData", customData));
    }

    public ID getOpUser() {
        return opUser;
    }

    public Permission getAction() {
        return action;
    }

    public ID getToUser() {
        return toUser;
    }

    public String[] getCascades() {
        return cascades == null ? ArrayUtils.EMPTY_STRING_ARRAY : cascades;
    }

    public ID[] getRecords() {
        return records;
    }

    public ID getTargetRecord() {
        return targetRecord;
    }

    public Map<String, Object> getExtraParams() {
        return extraParams;
    }

    public void addExtraParam(String name, Object value) {
        extraParams.put(name, value);
    }

    public Entity getMainEntity() {
        return mainEntity;
    }

    private Entity detecteMainEntity() {
        if (targetRecord != null) {
            return MetadataHelper.getEntity(targetRecord.getEntityCode());
        } else if (records != null && records.length > 0) {
            return MetadataHelper.getEntity(records[0].getEntityCode());
        } else if (extraParams.containsKey("customData")) {
            JSONObject customData = (JSONObject) extraParams.get("customData");
            return MetadataHelper.getEntity(customData.getString("entity"));
        }
        throw new RebuildException("No operation record");
    }
}
