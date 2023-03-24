/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import cn.devezhao.persist4j.engine.StandardRecord;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import cn.devezhao.persist4j.record.RecordVisitor;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyDateTime;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.PrivilegesGuardContextHolder;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerException;
import com.rebuild.core.service.trigger.TriggerResult;
import com.rebuild.core.service.trigger.aviator.AviatorUtils;
import com.rebuild.core.support.general.ContentWithFieldVars;
import com.rebuild.core.support.general.N2NReferenceSupport;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 字段更新，场景 1>1 1>N
 *
 * @author devezhao
 * @since 2020/2/7
 *
 * @see AutoFillinManager
 */
@Slf4j
public class FieldWriteback extends FieldAggregation {

    /**
     * 设：线索1、客户N（即 1 <:> N）
     * 当线索作为目标，客户作为源时，更新客户时只会更新 1 个线索（one2one）
     * 当客户作为目标，线索作为源时，更新线索时会更新 N 个客户
     */
    public static final String ONE2ONE_MODE = "one2one";

    private static final String DATE_EXPR = "#";
    private static final String CODE_PREFIX = "{{{{";  // ends with }}}}

    private Set<ID> targetRecordIds;
    private Record targetRecordData;

    public FieldWriteback(ActionContext context) {
        super(context, Boolean.TRUE);
    }

    @Override
    public ActionType getType() {
        return ActionType.FIELDWRITEBACK;
    }

    @Override
    public Object execute(OperatingContext operatingContext) throws TriggerException {
        final String chainName = String.format("%s:%s:%s", actionContext.getConfigId(),
                operatingContext.getAnyRecord().getPrimary(), operatingContext.getAction().getName());
        final List<String> tschain = checkTriggerChain(chainName);
        if (tschain == null) return TriggerResult.triggerOnce();

        this.prepare(operatingContext);

        if (targetRecordIds.isEmpty()) {
            log.debug("No target record(s) found");
            return TriggerResult.noMatching();
        }

        if (targetRecordData.isEmpty()) {
            log.info("No data of target record : {}", targetRecordIds);
            return TriggerResult.targetEmpty();
        }

        final boolean forceUpdate = ((JSONObject) actionContext.getActionContent()).getBooleanValue("forceUpdate");

        List<ID> affected = new ArrayList<>();
        boolean targetSame = false;

        for (ID targetRecordId : targetRecordIds) {
            if (operatingContext.getAction() == BizzPermission.DELETE
                    && targetRecordId.equals(operatingContext.getAnyRecord().getPrimary())) {
                // 删除时无需更新自己
                continue;
            }

            Record targetRecord = targetRecordData.clone();
            targetRecord.setID(targetEntity.getPrimaryField().getName(), targetRecordId);
            targetRecord.setDate(EntityHelper.ModifiedOn, CalendarUtils.now());

            // 相等则不更新
            if (isCurrentSame(targetRecord)) {
                log.info("Ignore execution because the record are same : {}", targetRecordId);
                targetSame = true;
                continue;
            }

            // 跳过权限
            PrivilegesGuardContextHolder.setSkipGuard(targetRecordId);

            // 强制更新 (v2.9)
            if (forceUpdate) {
                GeneralEntityServiceContextHolder.setAllowForceUpdate(targetRecordId);
            }

            // 重复检查模式
            GeneralEntityServiceContextHolder.setRepeatedCheckMode(GeneralEntityServiceContextHolder.RCM_CHECK_MAIN);

            List<String> tschainCurrentLoop = new ArrayList<>(tschain);
            tschainCurrentLoop.add(chainName);
            TRIGGER_CHAIN.set(tschainCurrentLoop);
            System.out.println("[dev] Use current-loop tschain : " + tschainCurrentLoop);

            try {
                Application.getBestService(targetEntity).createOrUpdate(targetRecord);
                affected.add(targetRecord.getPrimary());
            } finally {
                PrivilegesGuardContextHolder.getSkipGuardOnce();
                if (forceUpdate) GeneralEntityServiceContextHolder.isAllowForceUpdateOnce();
                GeneralEntityServiceContextHolder.getRepeatedCheckModeOnce();
            }
        }

        if (targetSame && affected.isEmpty()) return TriggerResult.targetSame();
        else return TriggerResult.success(affected);
    }

    @Override
    public void prepare(OperatingContext operatingContext) throws TriggerException {
        if (targetRecordIds != null) return;

        // FIELD.ENTITY
        String[] targetFieldEntity = ((JSONObject) actionContext.getActionContent()).getString("targetEntity").split("\\.");
        sourceEntity = actionContext.getSourceEntity();
        targetEntity = MetadataHelper.getEntity(targetFieldEntity[1]);

        // 1对1模式，此触发器还支持1对N
        boolean isOne2One = ((JSONObject) actionContext.getActionContent()).getBooleanValue(ONE2ONE_MODE);

        targetRecordIds = new HashSet<>();

        if (SOURCE_SELF.equalsIgnoreCase(targetFieldEntity[0])) {
            // 自己更新自己
            targetRecordIds.add(actionContext.getSourceRecord());

        }
        // 1>1
        else if (isOne2One) {
            Record afterRecord = operatingContext.getAfterRecord();
            if (afterRecord == null) return;

            if (afterRecord.hasValue(targetFieldEntity[0])) {
                Object o = afterRecord.getObjectValue(targetFieldEntity[0]);
                if (!NullValue.isNull(o)) {
                    // N2N
                    if (o instanceof ID[]) {
                        Collections.addAll(targetRecordIds, (ID[]) o);
                    } else {
                        targetRecordIds.add((ID) o);
                    }
                }
            } else {
                Object[] o = Application.getQueryFactory().uniqueNoFilter(afterRecord.getPrimary(),
                        targetFieldEntity[0], afterRecord.getEntity().getPrimaryField().getName());
                if (o != null && o[0] != null) {
                    // N2N
                    if (o[0] instanceof ID[]) {
                        Collections.addAll(targetRecordIds, (ID[]) o[0]);
                    } else {
                        targetRecordIds.add((ID) o[0]);
                    }
                }
            }
        }
        // 1>N
        else {
            // N2N v3.1
            Field targetField = targetEntity.getField(targetFieldEntity[0]);
            if (targetField.getType() == FieldType.REFERENCE_LIST) {
                Set<ID> set = N2NReferenceSupport.findReferences(targetField, operatingContext.getAnyRecord().getPrimary());
                targetRecordIds.addAll(set);
            } else {
                String sql = String.format("select %s from %s where %s = ?",
                        targetEntity.getPrimaryField().getName(), targetFieldEntity[1], targetFieldEntity[0]);
                Object[][] array = Application.createQueryNoFilter(sql)
                        .setParameter(1, operatingContext.getAnyRecord().getPrimary())
                        .array();

                for (Object[] o : array) {
                    targetRecordIds.add((ID) o[0]);
                }
            }
        }

        if (!targetRecordIds.isEmpty()) {
            targetRecordData = buildTargetRecordData();
        }
    }

    private Record buildTargetRecordData() {
        final Record targetRecord = EntityHelper.forNew(targetEntity.getEntityCode(), UserService.SYSTEM_USER, false);
        final JSONArray items = ((JSONObject) actionContext.getActionContent()).getJSONArray("items");

        final Set<String> fieldVars = new HashSet<>();
        final Set<String> fieldVarsN2NPath = new HashSet<>();
        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            String sourceField = item.getString("sourceField");
            String updateMode = item.getString("updateMode");
            // fix: v2.2
            if (updateMode == null) {
                updateMode = sourceField.contains(DATE_EXPR) ? "FORMULA" : "FIELD";
            }

            if ("FIELD".equalsIgnoreCase(updateMode)) {
                fieldVars.add(sourceField);
            } else if ("FORMULA".equalsIgnoreCase(updateMode)) {
                if (sourceField.contains(DATE_EXPR) && !sourceField.startsWith(CODE_PREFIX)) {
                    fieldVars.add(sourceField.split(DATE_EXPR)[0]);
                } else {
                    Set<String> matchsVars = ContentWithFieldVars.matchsVars(sourceField);
                    for (String field : matchsVars) {
                        // v3.3
                        if (isN2NField2Path(sourceEntity, field)) {
                            fieldVarsN2NPath.add(field);
                        } else {
                            if (MetadataHelper.getLastJoinField(sourceEntity, field) == null) {
                                throw new MissingMetaExcetion(field, sourceEntity.getName());
                            }
                            fieldVars.add(field);
                        }
                    }
                }
            }
        }

        // 变量值
        Record useSourceData = null;
        if (!fieldVars.isEmpty()) {
            String sql = MessageFormat.format("select {0},{1} from {2} where {1} = ?",
                    StringUtils.join(fieldVars, ","),
                    sourceEntity.getPrimaryField().getName(),
                    sourceEntity.getName());
            useSourceData = Application.createQueryNoFilter(sql).setParameter(1, actionContext.getSourceRecord()).record();
        }
        if (!fieldVarsN2NPath.isEmpty()) {
            if (useSourceData == null) useSourceData = new StandardRecord(sourceEntity, null);
            fieldVars.addAll(fieldVarsN2NPath);

            for (String field : fieldVarsN2NPath) {
                Object[] n2nVal = N2NReferenceSupport.getN2NValueByMixPath(field, actionContext.getSourceRecord());
                useSourceData.setObjectValue(field, n2nVal);
            }
        }

        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            String targetField = item.getString("targetField");
            if (!MetadataHelper.checkAndWarnField(targetEntity, targetField)) {
                continue;
            }

            EasyField targetFieldEasy = EasyMetaFactory.valueOf(targetEntity.getField(targetField));

            String updateMode = item.getString("updateMode");
            String sourceAny = item.getString("sourceField");

            // 置空
            if ("VNULL".equalsIgnoreCase(updateMode)) {
                targetRecord.setNull(targetField);
            }

            // 固定值
            else if ("VFIXED".equalsIgnoreCase(updateMode)) {
                RecordVisitor.setValueByLiteral(targetField, sourceAny, targetRecord);
            }

            // 字段
            else if ("FIELD".equalsIgnoreCase(updateMode)) {
                Field sourceFieldMeta = MetadataHelper.getLastJoinField(sourceEntity, sourceAny);
                if (sourceFieldMeta == null) continue;

                Object value = Objects.requireNonNull(useSourceData).getObjectValue(sourceAny);

                if (value != null) {
                    Object newValue = EasyMetaFactory.valueOf(sourceFieldMeta)
                            .convertCompatibleValue(value, targetFieldEasy);
                    targetRecord.setObjectValue(targetField, newValue);
                }
            }

            // 公式
            else if ("FORMULA".equalsIgnoreCase(updateMode)) {
                if (useSourceData == null) {
                    log.warn("[useSourceData] is null, Set to empty");
                    useSourceData = new StandardRecord(sourceEntity, null);
                }

                // 高级公式代码
                final boolean useCode = sourceAny.startsWith(CODE_PREFIX);

                // 日期兼容 fix: v2.2
                if (sourceAny.contains(DATE_EXPR) && !useCode) {
                    String fieldName = sourceAny.split(DATE_EXPR)[0];
                    Field sourceField2 = MetadataHelper.getLastJoinField(sourceEntity, fieldName);
                    if (sourceField2 == null) continue;

                    Object value = useSourceData.getObjectValue(fieldName);
                    Object newValue = value == null ? null : ((EasyDateTime) EasyMetaFactory.valueOf(sourceField2))
                            .convertCompatibleValue(value, targetFieldEasy, sourceAny);
                    if (newValue != null) {
                        targetRecord.setObjectValue(targetField, newValue);
                    }
                }

                // 高级公式（会涉及各种类型的运算）
                // @see AggregationEvaluator#evalFormula
                else {
                    String clearFormula = useCode
                            ? sourceAny.substring(4, sourceAny.length() - 4)
                            : sourceAny
                                .replace("×", "*")
                                .replace("÷", "/")
                                .replace("`", "\"");  // compatible: v2.4

                    Map<String, Object> envMap = new HashMap<>();

                    for (String fieldName : fieldVars) {
                        String replace = "{" + fieldName + "}";
                        String replaceWhitQuote = "\"" + replace + "\"";
                        String replaceWhitQuoteSingle = "'" + replace + "'";
                        boolean forceUseQuote = false;

                        if (clearFormula.contains(replaceWhitQuote)) {
                            clearFormula = clearFormula.replace(replaceWhitQuote, fieldName);
                            forceUseQuote = true;
                        } else if (clearFormula.contains(replaceWhitQuoteSingle)) {
                            clearFormula = clearFormula.replace(replaceWhitQuoteSingle, fieldName);
                            forceUseQuote = true;
                        } else if (clearFormula.contains(replace)) {
                            clearFormula = clearFormula.replace(replace, fieldName);
                        } else {
                            continue;
                        }

                        Object value = useSourceData.getObjectValue(fieldName);

                        if (value instanceof Date) {
                            value = CalendarUtils.getUTCDateTimeFormat().format(value);
                        } else if (value == null) {
                            // 数字字段置 `0`
                            Field isNumberField = MetadataHelper.getLastJoinField(sourceEntity, fieldName);
                            // N2N 保持 `NULL`
                            Field isN2NField = sourceEntity.containsField(fieldName) ? sourceEntity.getField(fieldName) : null;
                            if (isNumberField != null
                                    && (isNumberField.getType() == FieldType.LONG || isNumberField.getType() == FieldType.DECIMAL)) {
                                value = 0;
                            } else if (fieldVarsN2NPath.contains(fieldName)
                                    || (isN2NField != null && isN2NField.getType() == FieldType.REFERENCE_LIST)) {
                                // Keep NULL
                            } else {
                                value = StringUtils.EMPTY;
                            }
                        } else if (value instanceof ID || forceUseQuote) {
                            value = value.toString();
                        }

                        envMap.put(fieldName, value);
                    }

                    Object newValue = AviatorUtils.eval(clearFormula, envMap, Boolean.FALSE);

                    if (newValue != null) {
                        DisplayType dt = targetFieldEasy.getDisplayType();
                        if (dt == DisplayType.NUMBER) {
                            targetRecord.setLong(targetField, CommonsUtils.toLongHalfUp(newValue));
                        } else if (dt == DisplayType.DECIMAL) {
                            targetRecord.setDouble(targetField, ObjectUtils.toDouble(newValue));
                        } else if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
                            targetRecord.setDate(targetField, (Date) newValue);
                        } else {
                            newValue = checkoutFieldValue(newValue, targetFieldEasy);
                            if (newValue != null) {
                                targetRecord.setObjectValue(targetField, newValue);
                            }
                        }
                    }
                }
            }
        }

        return targetRecord;
    }

    /**
     * @see DisplayType
     * @see com.rebuild.core.metadata.EntityRecordCreator
     */
    private Object checkoutFieldValue(Object value, EasyField field) {
        DisplayType dt = field.getDisplayType();
        Object newValue = null;

        if (dt == DisplayType.PICKLIST || dt == DisplayType.CLASSIFICATION
                || dt == DisplayType.REFERENCE || dt == DisplayType.ANYREFERENCE) {

            ID id = ID.isId(value) ? ID.valueOf(value.toString()) : null;
            if (id != null) {
                int idCode = id.getEntityCode();
                if (dt == DisplayType.PICKLIST) {
                    if (idCode == EntityHelper.PickList) newValue = id;
                } else if (dt == DisplayType.CLASSIFICATION) {
                    if (idCode == EntityHelper.ClassificationData) newValue = id;
                } else if (dt == DisplayType.REFERENCE) {
                    if (field.getRawMeta().getReferenceEntity().getEntityCode() == idCode) newValue = id;
                } else {
                    newValue = id;
                }
            }

        } else if (dt == DisplayType.N2NREFERENCE) {

            String[] ids = value.toString().split(",");
            List<String> idsList = new ArrayList<>();
            for (String id : ids) {
                if (ID.isId(id)) idsList.add(id);
            }
            if (ids.length == idsList.size()) newValue = value.toString();

        } else if (dt == DisplayType.BOOL) {

            if (value instanceof Boolean) {
                newValue = value;
            } else {
                newValue = BooleanUtils.toBooleanObject(value.toString());
            }

        } else if (dt == DisplayType.MULTISELECT || dt == DisplayType.STATE) {

            if (value instanceof Integer || value instanceof Long) {
                newValue = value;
            }

        } else {
            // TODO 验证字段格式
            newValue = value.toString();
        }

        if (newValue == null) {
            log.warn("Value `{}` cannot be convert to field (value) : {}", value, field.getRawMeta());
        }
        return newValue;
    }

    private boolean isN2NField2Path(Entity entity, String fieldPath) {
        String[] fields = fieldPath.split("\\.");
        if (fields.length < 2) return false;

        try {
            // N2N.F
            Field firstField = entity.getField(fields[0]);
            if (firstField.getType() == FieldType.REFERENCE_LIST) return true;
            // F.N2N
            return firstField.getReferenceEntity().getField(fields[1]).getType() == FieldType.REFERENCE_LIST;
        } catch (MissingMetaExcetion ex) {
            throw new MissingMetaExcetion(fieldPath, entity.getName());
        }
    }
}
