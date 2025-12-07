/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.transform;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.query.FilterRecordChecker;
import com.rebuild.core.service.query.ParseHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Zixin
 * @since 2024/4/8
 */
@Slf4j
public class RecordTransfomer37 extends RecordTransfomer {

    public RecordTransfomer37(ID transid) {
        super(transid);
    }

    public RecordTransfomer37(Entity targetEntity, JSONObject transConfig, boolean skipGuard) {
        super(targetEntity, transConfig, skipGuard);
    }

    @Override
    public ID transform(ID sourceRecordId) {
        return this.transform(sourceRecordId, null);
    }

    @Override
    public ID transform(ID sourceRecordId, ID specMainId) {
        // 主
        JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMapping");
        if (fieldsMapping == null || fieldsMapping.isEmpty()) {
            throw new ConfigurationException("INVALID CONFIG OF TRANSFORM");
        }
        // 兼容
        if (fieldsMapping.get("_") == null) {
            transConfig.put("checkNullable35", false);
            return super.transform(sourceRecordId, specMainId);
        }

        // ND:明细
        JSONArray fieldsMappingDetails = transConfig.getJSONArray("fieldsMappingDetails");
        // 兼容
        if (fieldsMappingDetails == null) {
            transConfig.put("checkNullable35", false);
            return super.transform(sourceRecordId, specMainId);
        }

        // v3.5 此配置未开放
        // 在之前的版本中，虽然文档写明非空字段无值会转换失败，但是从来没有做过非空检查
        // 为保持兼容性，此选项不启用，即入参保持为 false，如有需要可指定为 true
        final boolean checkNullable = transConfig.getBooleanValue("checkNullable35");

        List<Record> detailsList = new ArrayList<>();
        for (Object o : fieldsMappingDetails) {
            JSONObject fmd = (JSONObject) o;
            Entity[] fmdEntity = checkEntity(fmd);
            if (fmdEntity == null) continue;

            Entity dTargetEntity = fmdEntity[0];
            Entity dSourceEntity = fmdEntity[1];

            String querySourceSql = buildDetailsSourceSql(dSourceEntity, sourceRecordId);
            Object[][] dArray = Application.createQueryNoFilter(querySourceSql).array();
            // be:4.2.5 支持字段变量
            JSONObject transFilter = getTransFilter(fmd);
            FilterRecordChecker transChecker = transFilter != null ? new FilterRecordChecker(transFilter) : null;

            Map<String, Object> dvMap4Details = Collections.singletonMap(
                    MetadataHelper.getDetailToMainField(dTargetEntity).getName(), EntityHelper.UNSAVED_ID);

            for (Object[] d : dArray) {
                if (transChecker != null) {
                    if (!transChecker.check((ID) d[0])) continue;
                }

                Record dRecord = transformRecord(
                        dSourceEntity, dTargetEntity, fmd, (ID) d[0], dvMap4Details, false, false, checkNullable);
                detailsList.add(dRecord);
            }
        }

        // 目标是明细
        String dtmField = null;
        Map<String, Object> dvMap4Details = null;
        if (specMainId != null) {
            dtmField = MetadataHelper.getDetailToMainField(targetEntity).getName();
            dvMap4Details = Collections.singletonMap(dtmField, specMainId);
        }

        Entity sourceEntity = MetadataHelper.getEntity(sourceRecordId.getEntityCode());
        Record targetRecord = transformRecord(
                sourceEntity, targetEntity, fieldsMapping, sourceRecordId, dvMap4Details, false, false, checkNullable);

        // 明细强制修订，以免做过映射字段导致不对
        if (specMainId != null) targetRecord.setID(dtmField, specMainId);

        // v3.5 需要先回填
        // 因为可能以回填字段作为条件进行转换一次判断
        boolean fillbackFix = fillback(sourceRecordId, EntityHelper.newUnsavedId(targetRecord.getEntity().getEntityCode()));

        // 保存
        ID theNewId = saveRecord(targetRecord, detailsList.isEmpty() ? null : detailsList);

        // 回填修正
        if (fillbackFix) fillback(sourceRecordId, theNewId);

        return theNewId;
    }

    /**
     * @param fmd
     * @return Returns [Tatget, Source]
     */
    protected static Entity[] checkEntity(JSONObject fmd) {
        JSONObject fmdMeta = fmd.getJSONObject("_");
        if (fmdMeta == null) throw new ConfigurationException("INCOMPATIBLE(39) TRANSFORM CONFIG");

        String dTargetEntity = fmdMeta.getString("target");
        if (!MetadataHelper.containsEntity(dTargetEntity)) {
            log.warn("Tatget entity not longer exists : {}", dTargetEntity);
            return null;
        }

        String dSourceEntity = fmdMeta.getString("source");
        if (!MetadataHelper.containsEntity(dSourceEntity)) {
            log.warn("Source entity not longer exists : {}", dSourceEntity);
            return null;
        }

        return new Entity[] { MetadataHelper.getEntity(dTargetEntity), MetadataHelper.getEntity(dSourceEntity) };
    }

    /**
     * 获取转换过滤
     *
     * @param fmd
     * @return
     */
    protected static JSONObject getTransFilter(JSONObject fmd) {
        JSONObject fmdMeta = fmd.getJSONObject("_");
        if (fmdMeta == null) return null;

        JSONObject hasFilter = fmdMeta.getJSONObject("filter");
        return ParseHelper.validAdvFilter(hasFilter) ? hasFilter : null;
    }

    /**
     * 构建明细查询语句
     *
     * @param sourceEntity
     * @param sourceId
     * @return
     */
    protected static String buildDetailsSourceSql(Entity sourceEntity, ID sourceId) {
        String querySourceSql = null;
        if (sourceEntity.getMainEntity() != null) {
            querySourceSql = String.format(
                    "select %s from %s where %s = '%s' and (1=1) order by autoId asc",
                    sourceEntity.getPrimaryField().getName(), sourceEntity.getName(),
                    MetadataHelper.getDetailToMainField(sourceEntity).getName(), sourceId);
        }

        // 明细 > 主+明细
        if (querySourceSql == null || sourceEntity.getEntityCode().equals(sourceId.getEntityCode())) {
            querySourceSql = String.format(
                    "select %s from %s where %s = '%s' and (1=1) order by autoId asc",
                    sourceEntity.getPrimaryField().getName(), sourceEntity.getName(),
                    sourceEntity.getPrimaryField().getName(), sourceId);
        }

        return querySourceSql;
    }
}
