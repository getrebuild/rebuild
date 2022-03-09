/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.transform;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.TransactionManual;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.query.FilterRecordChecker;
import com.rebuild.core.support.SetUser;
import com.rebuild.core.support.general.N2NReferenceSupport;
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

    /**
     * @param targetEntity
     * @param transConfig
     */
    public RecordTransfomer(Entity targetEntity, JSONObject transConfig) {
        this.targetEntity = targetEntity;
        this.transConfig = transConfig;
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
     * @see #checkFilter(ID)
     */
    public ID transform(ID sourceRecordId) {
        // 手动事务，因为可能要转换多条记录
        TransactionStatus tx = TransactionManual.newTransaction();

        try {
            // 主记录

            JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMapping");
            if (fieldsMapping == null || fieldsMapping.isEmpty()) {
                throw new ConfigurationException("Invalid config of transform : " + transConfig);
            }

            final Entity sourceEntity = MetadataHelper.getEntity(sourceRecordId.getEntityCode());
            final ID newId = transformRecord(sourceEntity, targetEntity, fieldsMapping, sourceRecordId, null);
            if (newId == null) {
                throw new ConfigurationException("Cannot transform record of main : " + transConfig);
            }

            // 明细

            JSONObject fieldsMappingDetail = transConfig.getJSONObject("fieldsMappingDetail");
            if (fieldsMappingDetail != null && !fieldsMappingDetail.isEmpty()) {
                Entity sourceDetailEntity = sourceEntity.getDetailEntity();
                Field sourceDtf = MetadataHelper.getDetailToMainField(sourceDetailEntity);

                String sql = String.format(
                        "select %s from %s where %s = '%s'",
                        sourceDetailEntity.getPrimaryField().getName(), sourceDetailEntity.getName(), sourceDtf.getName(), sourceRecordId);
                Object[][] details = Application.createQueryNoFilter(sql).array();

                Entity targetDetailEntity = targetEntity.getDetailEntity();
                Map<String, Object> map = null;
                if (details.length > 0) {
                    Field targetDtf = MetadataHelper.getDetailToMainField(targetDetailEntity);
                    map = Collections.singletonMap(targetDtf.getName(), newId);
                }

                for (Object[] o : details) {
                    transformRecord(sourceDetailEntity, targetDetailEntity, fieldsMappingDetail, (ID) o[0], map);
                }
            }

            // 回填

            String fillbackField = transConfig.getString("fillbackField");
            if (StringUtils.isNotBlank(fillbackField) && MetadataHelper.checkAndWarnField(sourceEntity, fillbackField)) {
                Record updateSource = EntityHelper.forUpdate(sourceRecordId, getUser(), false);
                updateSource.setID(fillbackField, newId);

                // TODO 此配置未开放
                int fillbackMode = transConfig.getIntValue("fillbackMode");

                // 仅更新，无业务规则
                if (fillbackMode == 3) {
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
            }

            TransactionManual.commit(tx);
            return newId;

        } catch (Exception ex) {
            TransactionManual.rollback(tx);
            throw ex;
        }
    }

    private ID transformRecord(
            Entity sourceEntity, Entity targetEntity, JSONObject fieldsMapping,
            ID sourceRecordId, Map<String, Object> defaultValue) {

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

        String querySource = String.format(
                "select %s from %s where %s = '%s'",
                StringUtils.join(validFields, ","), sourceEntity.getName(),
                sourceEntity.getPrimaryField().getName(), sourceRecordId);
        Record source = Application.createQueryNoFilter(querySource).record();

        for (Map.Entry<String, Object> e : fieldsMapping.entrySet()) {
            if (e.getValue() == null) continue;

            String targetField = e.getKey();
            String sourceField = (String) e.getValue();

            Object sourceValue = source.getObjectValue(sourceField);
            if (sourceValue != null) {
                EasyField targetFieldEasy = EasyMetaFactory.valueOf(targetEntity.getField(targetField));
                EasyField sourceFieldEasy = EasyMetaFactory.valueOf(sourceEntity.getField(sourceField));

                if (targetFieldEasy.getDisplayType() == DisplayType.N2NREFERENCE) {
                    sourceValue = N2NReferenceSupport.items(sourceFieldEasy.getRawMeta(), sourceRecordId);
                }

                Object targetValue = sourceFieldEasy.convertCompatibleValue(sourceValue, targetFieldEasy);
                target.setObjectValue(targetField, targetValue);
            }
        }

        target = Application.getEntityService(targetEntity.getEntityCode()).create(target);
        return target.getPrimary();
    }

    private List<String> checkAndWarnFields(Entity entity, Collection<?> fieldsName) {
        List<String> valid = new ArrayList<>();
        for (Object field : fieldsName) {
            if (field == null) continue;

            if (MetadataHelper.checkAndWarnField(entity, (String) field)) {
                valid.add((String) field);
            }
        }
        return valid;
    }
}
