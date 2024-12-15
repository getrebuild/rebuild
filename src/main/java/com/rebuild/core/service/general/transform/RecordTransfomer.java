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
import cn.devezhao.persist4j.record.RecordVisitor;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.EntityRecordCreator;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.GeneralEntityService;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.query.FilterRecordChecker;
import com.rebuild.core.support.SetUser;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 转换记录
 * 1. 转换主记录
 * 2. 转换主记录+（多条）明细记录
 * 3. 转换明细记录 > 主实体
 *
 * @author devezhao
 * @since 2020/10/27
 */
@Slf4j
public class RecordTransfomer extends SetUser {

    final protected Entity targetEntity;
    final protected JSONObject transConfig;
    final protected boolean skipGuard;

    /**
     * @param transid
     */
    public RecordTransfomer(ID transid) {
        ConfigBean config = TransformManager.instance.getTransformConfig(transid, null);
        this.targetEntity = MetadataHelper.getEntity(config.getString("target"));
        this.transConfig = (JSONObject) config.getJSON("config");
        this.skipGuard = false;
    }

    /**
     * @param targetEntity
     * @param transConfig
     * @param skipGuard 跳过权限
     */
    public RecordTransfomer(Entity targetEntity, JSONObject transConfig, boolean skipGuard) {
        this.targetEntity = targetEntity;
        this.transConfig = transConfig;
        this.skipGuard = skipGuard;
    }

    /**
     * 是否符合转换条件
     *
     * @param sourceRecordId
     * @return
     * @see FilterRecordChecker
     */
    public boolean checkFilter(ID sourceRecordId) {
        JSONObject useFilter = transConfig.getJSONObject("useFilter");
        return new FilterRecordChecker(useFilter).check(sourceRecordId);
    }

    /**
     * @param sourceRecordId
     * @return
     */
    public ID transform(ID sourceRecordId) {
        return transform(sourceRecordId, null);
    }

    /**
     * @param sourceRecordId
     * @param specMainId 转换明细时需指定主记录 ID
     * @return
     * @see #checkFilter(ID)
     */
    public ID transform(ID sourceRecordId, ID specMainId) {
        // 检查配置
        Entity sourceEntity = MetadataHelper.getEntity(sourceRecordId.getEntityCode());
        Entity sourceDetailEntity = null;

        // 主
        JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMapping");
        if (fieldsMapping == null || fieldsMapping.isEmpty()) {
            throw new ConfigurationException("Invalid config of transform : " + transConfig);
        }

        // 明细
        JSONObject fieldsMappingDetail = transConfig.getJSONObject("fieldsMappingDetail");
        Object[][] sourceDetails = null;
        if (fieldsMappingDetail != null && !fieldsMappingDetail.isEmpty()) {
            sourceDetailEntity = sourceEntity.getDetailEntity();
            Field sourceRefField;

            // v2.10 1条记录 > 2条记录（主+明细）
            if (sourceDetailEntity == null) {
                sourceDetailEntity = sourceEntity;
                sourceRefField = sourceDetailEntity.getPrimaryField();
            } else {
                sourceRefField = MetadataHelper.getDetailToMainField(sourceDetailEntity);
            }

            String sql = String.format(
                    "select %s from %s where %s = '%s' order by autoId asc",
                    sourceDetailEntity.getPrimaryField().getName(), sourceDetailEntity.getName(), sourceRefField.getName(), sourceRecordId);
            sourceDetails = Application.createQueryNoFilter(sql).array();
        }

        Map<String, Object> dvMap = null;
        if (specMainId != null) {
            Field targetDtf = MetadataHelper.getDetailToMainField(targetEntity);
            dvMap = Collections.singletonMap(targetDtf.getName(), specMainId);
        }

        // v3.5 此配置未开放
        // 在之前的版本中，虽然文档写明非空字段无值会转换失败，但是从来没有做过非空检查
        // 为保持兼容性，此选项不启用，即入参保持为 false，如有需要可指定为 true
        final boolean checkNullable = transConfig.getBooleanValue("checkNullable35");

        Record mainRecord = transformRecord(
                sourceEntity, targetEntity, fieldsMapping, sourceRecordId, dvMap, false, false, checkNullable);
        ID theNewId;

        // v3.5 需要先回填
        // 因为可能以回填字段作为条件进行转换一次判断
        final boolean fillbackFix = fillback(sourceRecordId, EntityHelper.newUnsavedId(mainRecord.getEntity().getEntityCode()));

        // 有多条（主+明细）
        if (sourceDetails != null && sourceDetails.length > 0) {
            Entity targetDetailEntity = targetEntity.getDetailEntity();
            List<Record> detailsList = new ArrayList<>();
            for (Object[] d : sourceDetails) {
                detailsList.add(
                        transformRecord(sourceDetailEntity, targetDetailEntity, fieldsMappingDetail, (ID) d[0], null, false, false, checkNullable));
            }

            theNewId = saveRecord(mainRecord, detailsList);
        } else {
            theNewId = saveRecord(mainRecord, null);
        }

        // 回填修正
        if (fillbackFix) fillback(sourceRecordId, theNewId);

        return theNewId;
    }

    /**
     * @param record
     * @param detailsList
     * @return
     */
    protected ID saveRecord(Record record, List<Record> detailsList) {
        if (this.skipGuard) GeneralEntityServiceContextHolder.setSkipGuard(EntityHelper.UNSAVED_ID);

        if (detailsList != null && !detailsList.isEmpty()) {
            record.setObjectValue(GeneralEntityService.HAS_DETAILS, detailsList);
            GeneralEntityServiceContextHolder.setRepeatedCheckMode(GeneralEntityServiceContextHolder.RCM_CHECK_DETAILS);
        } else {
            GeneralEntityServiceContextHolder.setRepeatedCheckMode(GeneralEntityServiceContextHolder.RCM_CHECK_ALL);
        }

        try {
            record = Application.getEntityService(targetEntity.getEntityCode()).createOrUpdate(record);
            return record.getPrimary();
        } finally {
            if (this.skipGuard) GeneralEntityServiceContextHolder.isSkipGuardOnce();
            GeneralEntityServiceContextHolder.getRepeatedCheckModeOnce();
        }
    }

    /**
     * @param sourceRecordId
     * @param newId
     * @return
     */
    protected boolean fillback(ID sourceRecordId, ID newId) {
        final Entity sourceEntity = MetadataHelper.getEntity(sourceRecordId.getEntityCode());
        String fillbackField = transConfig.getString("fillbackField");
        if (fillbackField == null || !MetadataHelper.checkAndWarnField(sourceEntity, fillbackField)) {
            return false;
        }

        Record updateSource = EntityHelper.forUpdate(sourceRecordId, UserService.SYSTEM_USER, false);
        updateSource.setID(fillbackField, newId);

        // 此配置未开放
        int fillbackMode = transConfig.getIntValue("fillbackMode");
        if (fillbackMode == 2 && !EntityHelper.isUnsavedId(newId)) {
            GeneralEntityServiceContextHolder.setAllowForceUpdate(updateSource.getPrimary());
            try {
                Application.getEntityService(sourceEntity.getEntityCode()).update(updateSource);
            } finally {
                GeneralEntityServiceContextHolder.isAllowForceUpdateOnce();
            }
        } else {
            // 无传播更新
            Application.getCommonsService().update(updateSource, false);
        }

        return true;
    }

    /**
     * 转换 Record
     *
     * @param sourceEntity
     * @param targetEntity
     * @param fieldsMapping
     * @param sourceRecordId
     * @param defaultValue
     * @param ignoreUncreateable 忽略不可新建字段
     * @param forceNullValue v3.5 强制设定空字段值（更新记录时）
     * @param checkNullable v3.5 检查不允许为空的字段是否都有值
     * @return
     */
    protected Record transformRecord(
            Entity sourceEntity, Entity targetEntity, JSONObject fieldsMapping,
            ID sourceRecordId, Map<String, Object> defaultValue, boolean ignoreUncreateable, boolean forceNullValue, boolean checkNullable) {
        // v3.7 clean
        fieldsMapping.remove("_");

        Record targetRecord = EntityHelper.forNew(targetEntity.getEntityCode(), getUser());

        if (defaultValue != null) {
            for (Map.Entry<String, Object> e : defaultValue.entrySet()) {
                targetRecord.setObjectValue(e.getKey(), e.getValue());
            }
        }

        List<String> validFields = checkAndWarnFields(sourceEntity, fieldsMapping.values());
        if (validFields.isEmpty()) {
            // fix: https://github.com/getrebuild/rebuild/issues/633
            log.warn("No fields (var) for transform : {}", fieldsMapping);
        }

        validFields.add(sourceEntity.getPrimaryField().getName());
        Record sourceRecord = Application.getQueryFactory().recordNoFilter(sourceRecordId, validFields.toArray(new String[0]));

        // 所属用户
        ID specOwningUser = null;

        for (Map.Entry<String, Object> e : fieldsMapping.entrySet()) {
            final String targetField = e.getKey();

            if (e.getValue() == null) {
                if (forceNullValue) targetRecord.setNull(targetField);
                continue;
            }

            EasyField targetFieldEasy = EasyMetaFactory.valueOf(targetEntity.getField(targetField));
            if (ignoreUncreateable && !targetFieldEasy.isCreatable()) continue;

            Object sourceAny = e.getValue();

            if (sourceAny instanceof JSONArray) {
                Object sourceValue = ((JSONArray) sourceAny).get(0);
                RecordVisitor.setValueByLiteral(targetField, sourceValue.toString(), targetRecord);

            } else {
                String sourceField = (String) sourceAny;
                Object sourceValue = sourceRecord.getObjectValue(sourceField);

                if (sourceValue != null) {
                    EasyField sourceFieldEasy = EasyMetaFactory.valueOf(
                            Objects.requireNonNull(MetadataHelper.getLastJoinField(sourceEntity, sourceField)));

                    Object targetValue = sourceFieldEasy.convertCompatibleValue(sourceValue, targetFieldEasy);
                    targetRecord.setObjectValue(targetField, targetValue);

                } else if (forceNullValue) {
                    targetRecord.setNull(targetField);
                }
            }

            if (EntityHelper.OwningUser.equals(targetField)) {
                specOwningUser = targetRecord.getID(EntityHelper.OwningUser);
            }
        }

        if (specOwningUser != null) {
            targetRecord.setID(EntityHelper.OwningDept,
                    (ID) Application.getUserStore().getUser(specOwningUser).getOwningDept().getIdentity());
        }

        if (checkNullable) {
            AutoFillinManager.instance.fillinRecord(targetRecord);
            new EntityRecordCreator(targetEntity, JSONUtils.EMPTY_OBJECT, getUser()).verify(targetRecord);
        }

        return targetRecord;
    }

    private List<String> checkAndWarnFields(Entity entity, Collection<?> fields) {
        List<String> valid = new ArrayList<>();
        for (Object field : fields) {
            if (field == null) continue;
            if (field instanceof JSONArray) continue;  // VFIXED
            if (field instanceof JSONObject) continue;  // `_`

            if (MetadataHelper.getLastJoinField(entity, (String) field) != null) {
                valid.add((String) field);
            } else {
                log.warn("Invalid field : {} in {}", field, entity.getName());
            }
        }
        return valid;
    }
}
