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
import com.rebuild.core.service.TransactionManual;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.query.FilterRecordChecker;
import com.rebuild.core.support.SetUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.TransactionStatus;

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
     * @param trnasid
     */
    public RecordTransfomer(ID trnasid) {
        ConfigBean config = TransformManager.instance.getTransformConfig(trnasid, null);
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
        // 手动事务，因为可能要转换多条记录
        TransactionStatus tx = TransactionManual.newTransaction();

        try {
            // 主记录

            Map<String, Object> map = null;
            if (mainId != null) {
                Field targetDtf = MetadataHelper.getDetailToMainField(targetEntity);
                map = Collections.singletonMap(targetDtf.getName(), mainId);
            }

            JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMapping");
            if (fieldsMapping == null || fieldsMapping.isEmpty()) {
                throw new ConfigurationException("Invalid config of transform : " + transConfig);
            }

            final Entity sourceEntity = MetadataHelper.getEntity(sourceRecordId.getEntityCode());
            final ID newId = saveRecord(sourceEntity, targetEntity, fieldsMapping, sourceRecordId, map);
            if (newId == null) {
                throw new ConfigurationException("Cannot transform record of main : " + transConfig);
            }

            // 明细记录（如有）

            JSONObject fieldsMappingDetail = transConfig.getJSONObject("fieldsMappingDetail");
            if (fieldsMappingDetail != null && !fieldsMappingDetail.isEmpty()) {
                Entity sourceDetailEntity = sourceEntity.getDetailEntity();
                Field sourceDtf = MetadataHelper.getDetailToMainField(sourceDetailEntity);

                String sql = String.format(
                        "select %s from %s where %s = '%s'",
                        sourceDetailEntity.getPrimaryField().getName(), sourceDetailEntity.getName(), sourceDtf.getName(), sourceRecordId);
                Object[][] details = Application.createQueryNoFilter(sql).array();

                Entity targetDetailEntity = targetEntity.getDetailEntity();
                if (details.length > 0) {
                    Field targetDtf = MetadataHelper.getDetailToMainField(targetDetailEntity);
                    map = Collections.singletonMap(targetDtf.getName(), newId);
                }

                for (Object[] o : details) {
                    saveRecord(sourceDetailEntity, targetDetailEntity, fieldsMappingDetail, (ID) o[0], map);
                }
            }

            // 回填
            fillback(sourceRecordId, newId);

            TransactionManual.commit(tx);
            return newId;

        } catch (Exception ex) {
            TransactionManual.rollback(tx);
            throw ex;
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
        if (StringUtils.isBlank(fillbackField)
                || !MetadataHelper.checkAndWarnField(sourceEntity, fillbackField)) {
            return false;
        }
        
        Record updateSource = EntityHelper.forUpdate(sourceRecordId, getUser(), false);
        updateSource.setID(fillbackField, newId);

        // TODO 此配置未开放
        int fillbackMode = transConfig.getIntValue("fillbackMode");

        // 仅更新，无业务规则
        if (fillbackMode == 3 || fillbackMode == 0) {
            Application.getCommonsService().update(updateSource, false);
        }
        // 忽略审批状态（进行中）强制更新
        else if (fillbackMode == 2) {
            GeneralEntityServiceContextHolder.setAllowForceUpdate(updateSource.getPrimary());
            try {
                Application.getEntityService(sourceEntity.getEntityCode()).update(updateSource);
            } finally {
                GeneralEntityServiceContextHolder.isAllowForceUpdateOnce();
            }
        }
        // 默认
        else {
            Application.getEntityService(sourceEntity.getEntityCode()).update(updateSource);
        }

        return true;
    }

    private ID saveRecord(
            Entity sourceEntity, Entity targetEntity, JSONObject fieldsMapping,
            ID sourceRecordId, Map<String, Object> defaultValue) {
        return (ID) transformRecord(sourceEntity, targetEntity, fieldsMapping, sourceRecordId, defaultValue, true);
    }

    /**
     * @param sourceEntity
     * @param targetEntity
     * @param fieldsMapping
     * @param sourceRecordId
     * @param defaultValue
     * @param save
     * @return Returns ID or Record
     */
    protected Object transformRecord(
            Entity sourceEntity, Entity targetEntity, JSONObject fieldsMapping,
            ID sourceRecordId, Map<String, Object> defaultValue, boolean save) {

        Record target = EntityHelper.forNew(targetEntity.getEntityCode(), getUser());

        if (defaultValue != null) {
            for (Map.Entry<String, Object> e : defaultValue.entrySet()) {
                target.setObjectValue(e.getKey(), e.getValue());
            }
        }

        List<String> validFields = checkAndWarnFields(sourceEntity, fieldsMapping.values());
        if (validFields.isEmpty()) {
            log.warn("No fields for transform");
            return null;
        }

        validFields.add(sourceEntity.getPrimaryField().getName());
        Record source = Application.getQueryFactory().recordNoFilter(sourceRecordId, validFields.toArray(new String[0]));

        for (Map.Entry<String, Object> e : fieldsMapping.entrySet()) {
            if (e.getValue() == null) continue;

            String targetField = e.getKey();
            EasyField targetFieldEasy = EasyMetaFactory.valueOf(targetEntity.getField(targetField));

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
        }

        if (!save) return target;

        if (this.skipGuard) {
            PrivilegesGuardContextHolder.setSkipGuard(EntityHelper.UNSAVED_ID);
        }

        GeneralEntityServiceContextHolder.setRepeatedCheckMode(GeneralEntityServiceContextHolder.RCM_CHECK_MAIN);
        try {
            target = Application.getEntityService(targetEntity.getEntityCode()).createOrUpdate(target);
            return target.getPrimary();
        } finally {
            GeneralEntityServiceContextHolder.getRepeatedCheckModeOnce();
            if (this.skipGuard) PrivilegesGuardContextHolder.getSkipGuardOnce();
        }
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
