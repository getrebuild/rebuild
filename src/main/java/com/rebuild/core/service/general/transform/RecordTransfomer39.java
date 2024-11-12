/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.transform;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.configuration.general.FormsBuilder;
import com.rebuild.core.configuration.general.FormsBuilderContextHolder;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.general.GeneralEntityService;
import com.rebuild.core.service.query.QueryHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Zixin
 * @since 2024/4/8
 */
@Slf4j
public class RecordTransfomer39 extends RecordTransfomer37 {

    private ID transid;
    volatile private ID targetRecordId;

    public RecordTransfomer39(ID transid) {
        super(transid);
        this.transid = transid;
    }

    /**
     * 转换
     *
     * @param sourceRecordId
     * @param specMainId
     * @param targetRecordId
     * @return
     */
    public ID transform(ID sourceRecordId, ID specMainId, ID targetRecordId) {
        this.targetRecordId = targetRecordId;

        ID theNewId = super.transform(sourceRecordId, specMainId);
        TransfomerTrace.trace(sourceRecordId, theNewId, transid, getUser());
        return theNewId;
    }

    @Override
    protected ID saveRecord(Record record, List<Record> detailsList) {
        if (targetRecordId == null) {
            return super.saveRecord(record, detailsList);
        }

        this.merge2Target(record);

        // 其下明细记录直接清空新建，因为没法一一对应去更新
        // 注意这会导致触发器触发动作不准
        JSONArray hasDetailsConf = transConfig.getJSONArray("fieldsMappingDetails");
        if (hasDetailsConf != null && !hasDetailsConf.isEmpty()) {
            List<ID> detailIds = QueryHelper.detailIdsNoFilter(targetRecordId);
            if (!detailIds.isEmpty()) {
                Application.getCommonsService().delete(detailIds.toArray(new ID[0]), true);
            }
        }

        return super.saveRecord(record, detailsList);
    }

    /**
     * 预览
     *
     * @param sourceRecordId
     * @param specMainId
     * @param targetRecordId
     * @return
     */
    public JSON preview(ID sourceRecordId, ID specMainId, ID targetRecordId) {
        this.targetRecordId = targetRecordId;

        Entity sourceEntity = MetadataHelper.getEntity(sourceRecordId.getEntityCode());
        ConfigBean config = TransformManager.instance.getTransformConfig(transid, sourceEntity.getName());
        JSONObject transConfig = (JSONObject) config.getJSON("config");

        JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMapping");
        JSONArray fieldsMappingDetails = transConfig.getJSONArray("fieldsMappingDetails");
        if (fieldsMapping == null || fieldsMapping.isEmpty()) {
            throw new ConfigurationException("INVALID TRANSFORM CONFIG");
        }
        if (fieldsMapping.get("_") == null) {
            throw new ConfigurationException("INCOMPATIBLE(39) TRANSFORM CONFIG");
        }

        Entity targetEntity = MetadataHelper.getEntity(config.getString("target"));
        Record targetRecord = transformRecord(sourceEntity, targetEntity, fieldsMapping, sourceRecordId,
                null, false, false, false);
        if (targetRecordId != null) {
            this.merge2Target(targetRecord);
        }

        TransformerPreview37.fillLabelOfReference(targetRecord);
        JSON formModel = UseFormsBuilder.buildNewFormWithRecord(targetEntity, targetRecord, specMainId, getUser());
        if (fieldsMappingDetails == null || fieldsMappingDetails.isEmpty()) return formModel;

        // 明细
        ID fakeMainid = EntityHelper.newUnsavedId(sourceEntity.getEntityCode());
        Map<String, JSONArray> formModelDetailsMap = new HashMap<>();
        for (Object o : fieldsMappingDetails) {
            JSONObject fmd = (JSONObject) o;
            Entity[] fmdEntity = checkEntity(fmd);
            if (fmdEntity == null) continue;

            Entity dTargetEntity = fmdEntity[0];
            Entity dSourceEntity = fmdEntity[1];

            String querySourceSql = buildDetailsSourceSql(dSourceEntity, sourceRecordId);
            String filter = appendFilter(fmd);
            if (filter != null) querySourceSql = querySourceSql.replace("(1=1)", filter);

            Object[][] dArray = Application.createQueryNoFilter(querySourceSql).array();

            JSONArray formModelDetails = new JSONArray();
            FormsBuilderContextHolder.setMainIdOfDetail(fakeMainid);
            try {
                for (Object[] d : dArray) {
                    Record targetRecordDetail = transformRecord(
                            dSourceEntity, dTargetEntity, fmd, (ID) d[0], null, true, false, false);
                    TransformerPreview37.fillLabelOfReference(targetRecordDetail);

                    JSON m = UseFormsBuilder.instance.buildNewForm(dTargetEntity, targetRecordDetail, FormsBuilder.DV_MAINID, getUser());
                    formModelDetails.add(m);
                }
                formModelDetailsMap.put(dTargetEntity.getName(), formModelDetails);

            } finally {
                FormsBuilderContextHolder.getMainIdOfDetail(true);
            }
        }

        ((JSONObject) formModel).put(GeneralEntityService.HAS_DETAILS, formModelDetailsMap);
        return formModel;
    }

    // 合并目标
    private void merge2Target(Record record) {
        JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMapping");
        Entity targetEntity = record.getEntity();

        record.setObjectValue(targetEntity.getPrimaryField().getName(), targetRecordId);
        record.removeValue(EntityHelper.CreatedBy);
        record.removeValue(EntityHelper.CreatedOn);
        if (!fieldsMapping.containsKey(EntityHelper.OwningUser)) {
            record.removeValue(EntityHelper.OwningUser);
            record.removeValue(EntityHelper.OwningDept);
        }

        // 清除空值（仅对配置了转换的字段有效）
        Record targetSnap = Application.getQueryFactory().recordNoFilter(targetRecordId);
        for (Field field : targetEntity.getFields()) {
            String fieldName = field.getName();
            if (fieldsMapping.containsKey(fieldName)) {
                if (!targetSnap.hasValue(fieldName)) {
                    record.setNull(fieldName);
                }
            }
        }
    }
}
