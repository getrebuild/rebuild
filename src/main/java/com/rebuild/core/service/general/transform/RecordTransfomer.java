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
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.EasyTag;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.GeneralEntityService;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.query.FilterRecordChecker;
import com.rebuild.core.support.RbvFunction;
import com.rebuild.core.support.SetUser;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.core.NamedThreadLocal;

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

    // 防止自动转换死循环
    private static final ThreadLocal<ID> FILLBACK2_ONCE414 = new NamedThreadLocal<>("FallbackMode=2 Trigger Once");

    final protected Entity targetEntity;
    final protected boolean skipGuard;

    final protected JSONObject transConfig;
    final protected ID transid;

    // v4.3
    private boolean checkSame = false;

    /**
     * @param transid
     */
    public RecordTransfomer(ID transid) {
        ConfigBean config = TransformManager.instance.getTransformConfig(transid, null);
        this.targetEntity = MetadataHelper.getEntity(config.getString("target"));
        this.transConfig = (JSONObject) config.getJSON("config");
        this.skipGuard = false;
        this.transid = transid;
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
        this.transid = null;
    }

    /**
     * 更新时忽略同值
     *
     * @param checkSame
     */
    protected void setCheckSame(boolean checkSame) {
        this.checkSame = checkSame;
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
        FILLBACK2_ONCE414.remove();

        // 检查配置
        Entity sourceEntity = MetadataHelper.getEntity(sourceRecordId.getEntityCode());
        Entity sourceDetailEntity = null;

        // 主
        JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMapping");
        if (MapUtils.isEmpty(fieldsMapping)) {
            throw new ConfigurationException("Invalid config of transform : " + transConfig);
        }

        // 明细
        JSONObject fieldsMappingDetail = transConfig.getJSONObject("fieldsMappingDetail");
        Object[][] sourceDetails = null;
        if (MapUtils.isNotEmpty(fieldsMappingDetail)) {
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
                Record dRecord = transformRecord(
                        sourceDetailEntity, targetDetailEntity, fieldsMappingDetail, (ID) d[0], null, false, false, checkNullable);
                detailsList.add(dRecord);
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

        if (CollectionUtils.isNotEmpty(detailsList)) {
            record.setObjectValue(GeneralEntityService.HAS_DETAILS, detailsList);
            GeneralEntityServiceContextHolder.setRepeatedCheckMode(GeneralEntityServiceContextHolder.RCM_CHECK_DETAILS);
        } else {
            GeneralEntityServiceContextHolder.setRepeatedCheckMode(GeneralEntityServiceContextHolder.RCM_CHECK_ALL);
        }

        if (checkSame && record.getPrimary() != null) {
            record = RbvFunction.call().restRecord(record);
            if (record.isEmpty()) return record.getPrimary();
        }

        try {
            record = Application.getBestService(targetEntity).createOrUpdate(record);
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
        return fillback(sourceRecordId, new ID[]{newId});
    }

    /**
     * @param sourceRecordId
     * @param newIds
     * @return
     */
    protected boolean fillback(ID sourceRecordId, ID[] newIds) {
        final Entity sourceEntity = MetadataHelper.getEntity(sourceRecordId.getEntityCode());
        String fillbackField = transConfig.getString("fillbackField");
        if (fillbackField == null || !MetadataHelper.checkAndWarnField(sourceEntity, fillbackField)) {
            return false;
        }

        // 更新源纪录
        Record s = EntityHelper.forUpdate(sourceRecordId, UserService.SYSTEM_USER, false);
        if (EasyMetaFactory.getDisplayType(s.getEntity().getField(fillbackField)) == DisplayType.N2NREFERENCE) {
            s.setIDArray(fillbackField, newIds);
        } else {
            s.setID(fillbackField, newIds[0]);
        }

        // 4.1.4 (LAB) 配置开放
        int fillbackMode = transConfig.getIntValue("fillbackMode");
        if (fillbackMode == 2 && !EntityHelper.isUnsavedId(newIds) && FILLBACK2_ONCE414.get() == null) {
            GeneralEntityServiceContextHolder.setAllowForceUpdate(s.getPrimary());
            FILLBACK2_ONCE414.set(newIds[0]);
            try {
                Application.getEntityService(sourceEntity.getEntityCode()).update(s);
            } finally {
                GeneralEntityServiceContextHolder.isAllowForceUpdateOnce();
            }
        } else {
            // 无传播更新
            Application.getBaseService().update(s);
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
            log.debug("No fields (var) for transform : {} in {}", fieldsMapping, this.transid);
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
                // fix:4.3
                if (targetFieldEasy.getDisplayType() == DisplayType.TAG) {
                    sourceValue = sourceValue.toString().replace(", ", EasyTag.VALUE_SPLIT);
                }

                EntityRecordCreator.setValueByLiteral(
                        targetFieldEasy.getRawMeta(), sourceValue.toString(), targetRecord, false);

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

            // v4.2 用户密码特殊处理
            if (targetEntity.getEntityCode() == EntityHelper.User && !targetRecord.hasValue("password")) {
                targetRecord.setString("password", CommonsUtils.randomHex().substring(0, 6) + "rB!8");
            }
            // v3.9 直接转换时验证非空字段
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
