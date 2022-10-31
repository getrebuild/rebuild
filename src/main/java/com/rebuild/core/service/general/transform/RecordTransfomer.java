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
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.PrivilegesGuardContextHolder;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.GeneralEntityService;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.query.FilterRecordChecker;
import com.rebuild.core.support.SetUser;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

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

    final private Entity targetEntity;
    final private JSONObject transConfig;
    final private boolean skipGuard;

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
     * @param mainId
     * @return
     * @see #checkFilter(ID)
     */
    public ID transform(ID sourceRecordId, ID mainId) {
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
        if (mainId != null) {
            Field targetDtf = MetadataHelper.getDetailToMainField(targetEntity);
            dvMap = Collections.singletonMap(targetDtf.getName(), mainId);
        }

        Record main = transformRecord(sourceEntity, targetEntity, fieldsMapping, sourceRecordId, dvMap, false);
        ID newId;

        // 有多条（主+明细）
        if (sourceDetails != null && sourceDetails.length > 0) {
            Entity targetDetailEntity = targetEntity.getDetailEntity();
            List<Record> detailsList = new ArrayList<>();
            for (Object[] d : sourceDetails) {
                detailsList.add(
                        transformRecord(sourceDetailEntity, targetDetailEntity, fieldsMappingDetail, (ID) d[0], null, false));
            }

            newId = saveRecord(main, detailsList);
        } else {
            newId = saveRecord(main, null);
        }

        // 回填
        fillback(sourceRecordId, newId);

        return newId;
    }

    private ID saveRecord(Record record, List<Record> detailsList) {
        if (this.skipGuard) {
            PrivilegesGuardContextHolder.setSkipGuard(EntityHelper.UNSAVED_ID);
        }

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
            GeneralEntityServiceContextHolder.getRepeatedCheckModeOnce();
            if (this.skipGuard) PrivilegesGuardContextHolder.getSkipGuardOnce();
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
        if (fillbackMode == 2) {
            GeneralEntityServiceContextHolder.setAllowForceUpdate(updateSource.getPrimary());
            try {
                Application.getEntityService(sourceEntity.getEntityCode()).update(updateSource);
            } finally {
                GeneralEntityServiceContextHolder.isAllowForceUpdateOnce();
            }
        } else {
            // FIXME 回填仅更新，无业务规则
            Application.getCommonsService().update(updateSource, false);
        }

        return true;
    }

    /**
     * 转换
     *
     * @param sourceEntity
     * @param targetEntity
     * @param fieldsMapping
     * @param sourceRecordId
     * @param defaultValue
     * @param ignoreUncreateable
     * @return
     */
    protected Record transformRecord(
            Entity sourceEntity, Entity targetEntity, JSONObject fieldsMapping,
            ID sourceRecordId, Map<String, Object> defaultValue, boolean ignoreUncreateable) {

        Record target = EntityHelper.forNew(targetEntity.getEntityCode(), getUser());

        if (defaultValue != null) {
            for (Map.Entry<String, Object> e : defaultValue.entrySet()) {
                target.setObjectValue(e.getKey(), e.getValue());
            }
        }

        List<String> validFields = checkAndWarnFields(sourceEntity, fieldsMapping.values());
        if (validFields.isEmpty()) {
            log.warn("No fields for transform : {}", fieldsMapping);
            return null;
        }

        validFields.add(sourceEntity.getPrimaryField().getName());
        Record source = Application.getQueryFactory().recordNoFilter(sourceRecordId, validFields.toArray(new String[0]));

        // 所属用户
        ID specOwningUser = null;

        for (Map.Entry<String, Object> e : fieldsMapping.entrySet()) {
            if (e.getValue() == null) continue;

            String targetField = e.getKey();
            EasyField targetFieldEasy = EasyMetaFactory.valueOf(targetEntity.getField(targetField));
            if (ignoreUncreateable && !targetFieldEasy.isCreatable()) continue;

            Object sourceAny = e.getValue();

            if (sourceAny instanceof JSONArray) {
                Object sourceValue = ((JSONArray) sourceAny).get(0);
                RecordVisitor.setValueByLiteral(targetField, sourceValue.toString(), target);

            } else {
                String sourceField = (String) sourceAny;
                Object sourceValue = source.getObjectValue(sourceField);

                if (sourceValue != null) {
                    EasyField sourceFieldEasy = EasyMetaFactory.valueOf(
                            Objects.requireNonNull(MetadataHelper.getLastJoinField(sourceEntity, sourceField)));

                    Object targetValue = sourceFieldEasy.convertCompatibleValue(sourceValue, targetFieldEasy);
                    target.setObjectValue(targetField, targetValue);
                }
            }

            if (EntityHelper.OwningUser.equals(targetField)) {
                specOwningUser = target.getID(EntityHelper.OwningUser);
            }
        }

        if (specOwningUser != null) {
            target.setID(EntityHelper.OwningDept,
                    (ID) Application.getUserStore().getUser(specOwningUser).getOwningDept().getIdentity());
        }

        return target;
    }

    private List<String> checkAndWarnFields(Entity entity, Collection<?> fields) {
        List<String> valid = new ArrayList<>();
        for (Object field : fields) {
            if (field == null) continue;
            if (field instanceof JSONArray) continue;  // VFIXED

            if (MetadataHelper.getLastJoinField(entity, (String) field) != null) {
                valid.add((String) field);
            } else {
                log.warn("Invalid field : {} in {}", field, entity.getName());
            }
        }
        return valid;
    }
}
