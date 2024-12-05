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
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.general.RecordDifference;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.RobotTriggerObserver;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerException;
import com.rebuild.core.service.trigger.TriggerResult;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 字段聚合，场景 N>1
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/29
 */
@Slf4j
public class FieldAggregation extends TriggerAction {

    private FieldAggregationRefresh fieldAggregationRefresh;

    /**
     * 最大触发链深度
     */
    protected static final int MAX_TRIGGER_DEPTH = CommandArgs.getInt(CommandArgs._TriggerMaxDepth, 256);

    // 此触发器可能产生连锁反应
    // 如触发器 A 调用 B，而 B 又调用了 C ... 以此类推。此处记录其深度
    protected static final ThreadLocal<List<String>> TRIGGER_CHAIN = new ThreadLocal<>();

    // 忽略更新数据库中相等记录
    final private boolean ignoreSame;

    // 源实体
    protected Entity sourceEntity;
    // 目标实体
    protected Entity targetEntity;

    // 目标记录
    protected ID targetRecordId;
    // 关联字段条件
    protected String followSourceWhere;

    transient private TargetWithMatchFields targetWithMatchFields;

    public FieldAggregation(ActionContext context) {
        this(context, Boolean.TRUE);
    }

    protected FieldAggregation(ActionContext context, boolean ignoreSame) {
        super(context);
        this.ignoreSame = ignoreSame;
    }

    @Override
    public ActionType getType() {
        return ActionType.FIELDAGGREGATION;
    }

    @Override
    public void clean() {
        super.clean();

        if (fieldAggregationRefresh != null) {
            log.info("Clear after refresh : {}", fieldAggregationRefresh);
            fieldAggregationRefresh.refresh();
            fieldAggregationRefresh = null;
        }
    }

    /**
     * 检查调用链
     *
     * @param chainName
     * @return
     */
    protected List<String> checkTriggerChain(String chainName) {
        List<String> tschain = TRIGGER_CHAIN.get();
        if (tschain == null) {
            tschain = new ArrayList<>();
            if (CommonsUtils.DEVLOG) System.out.println("[dev] New trigger-chain : " + this);
        } else {
            String w = String.format("Occured trigger-chain : %s > %s (current)", StringUtils.join(tschain, " > "), chainName);

            // 在整个触发链上只触发1次，避免循环调用
            // FIXME 20220804 某些场景是否允许2次，而非1次???
            if (tschain.contains(chainName)) {
                log.warn("{}!!! TRIGGER ONCE ONLY", w);
                return null;
            } else {
                log.info(w);
            }
        }

        if (tschain.size() >= MAX_TRIGGER_DEPTH) {
            throw new TriggerException("Exceed the maximum trigger depth : " + StringUtils.join(tschain, " > "));
        }

        return tschain;
    }

    @Override
    public Object execute(OperatingContext operatingContext) throws TriggerException {
        final String chainName = String.format("%s:%s:%s", actionContext.getConfigId(),
                operatingContext.getFixedRecordId(), operatingContext.getAction().getName());
        final List<String> tschain = checkTriggerChain(chainName);
        if (tschain == null) return TriggerResult.triggerOnce();

        this.prepare(operatingContext);

        if (targetRecordId == null) {
            log.info("No target record found");
            return TriggerResult.noMatching();
        }

        if (!QueryHelper.exists(targetRecordId)) {
            log.warn("Target record dose not exists: {} (On {})", targetRecordId, actionContext.getConfigId());
            return TriggerResult.targetNotExists();
        }

        // 聚合数据过滤
        JSONObject dataFilter = ((JSONObject) actionContext.getActionContent()).getJSONObject("dataFilter");
        String dataFilterSql = null;
        if (ParseHelper.validAdvFilter(dataFilter)) {
            dataFilterSql = new AdvFilterParser(dataFilter, operatingContext.getFixedRecordId()).toSqlWhere();
        }

        String filterSql = followSourceWhere;
        if (dataFilterSql != null) {
            filterSql = String.format("( %s ) and ( %s )", followSourceWhere, dataFilterSql);
        }

        // 构建目标记录数据
        Record targetRecord = EntityHelper.forUpdate(targetRecordId, UserService.SYSTEM_USER, false);

        JSONArray items = ((JSONObject) actionContext.getActionContent()).getJSONArray("items");
        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            String targetField = item.getString("targetField");
            if (!MetadataHelper.checkAndWarnField(targetEntity, targetField)) continue;

            Object evalValue = new AggregationEvaluator(item, sourceEntity, filterSql).eval();
            if (evalValue == null) continue;

            DisplayType dt = EasyMetaFactory.getDisplayType(targetEntity.getField(targetField));
            if (dt == DisplayType.NUMBER) {
                targetRecord.setLong(targetField, CommonsUtils.toLongHalfUp(evalValue));

            } else if (dt == DisplayType.DECIMAL) {
                targetRecord.setDouble(targetField, ObjectUtils.toDouble(evalValue));

            } else if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
                if (evalValue instanceof Date) targetRecord.setDate(targetField, (Date) evalValue);
                else targetRecord.setNull(targetField);

            } else if (dt == DisplayType.NTEXT || dt == DisplayType.N2NREFERENCE || dt == DisplayType.FILE) {
                Object[] oArray = (Object[]) evalValue;

                if (oArray.length == 0) {
                    targetRecord.setNull(targetField);
                } else if (dt == DisplayType.NTEXT) {
                    // ID 则使用文本
                    if (oArray[0] instanceof ID) {
                        List<String> labelList = new ArrayList<>();
                        for (Object id : oArray) {
                            labelList.add(FieldValueHelper.getLabelNotry((ID) id));
                        }
                        oArray = labelList.toArray(new String[0]);
                    }

                    String join = StringUtils.join(oArray, ", ");
                    targetRecord.setString(targetField, join);

                } else if (dt == DisplayType.N2NREFERENCE) {
                    // 强制去重
                    Set<ID> idSet = new LinkedHashSet<>();
                    for (Object id : oArray) {
                        if (id instanceof ID) idSet.add((ID) id);
                        else idSet.add(ID.valueOf((String) id));  // 主键会保持文本
                    }
                    targetRecord.setIDArray(targetField, idSet.toArray(new ID[0]));

                } else {
                    String join = JSON.toJSONString(oArray);
                    targetRecord.setString(targetField, join);
                }

            } else {
                log.warn("Unsupported file-type {} with {}", dt, targetRecordId);
            }
        }

        // 有需要才执行
        if (targetRecord.isEmpty()) {
            if (!RobotTriggerObserver._TriggerLessLog) log.info("No data of target record : {}", targetRecordId);
            return TriggerResult.targetEmpty();
        }

        // 相等则不更新
        if (isCurrentSame(targetRecord)) {
            if (!RobotTriggerObserver._TriggerLessLog) log.info("Ignore execution because the record are same : {}", targetRecordId);
            return TriggerResult.targetSame();
        }

        final boolean forceUpdate = ((JSONObject) actionContext.getActionContent()).getBooleanValue("forceUpdate");
        final boolean stopPropagation = ((JSONObject) actionContext.getActionContent()).getBooleanValue("stopPropagation");

        // 跳过权限
        GeneralEntityServiceContextHolder.setSkipGuard(targetRecordId);

        // 强制更新 (v2.9)
        if (forceUpdate) {
            GeneralEntityServiceContextHolder.setAllowForceUpdate(targetRecordId);
        }
        // 快速模式 (v3.8)
        if (stopPropagation) {
            GeneralEntityServiceContextHolder.setQuickMode();
        }

        tschain.add(chainName);
        TRIGGER_CHAIN.set(tschain);

        targetRecord.setDate(EntityHelper.ModifiedOn, CalendarUtils.now());
        targetRecord.setID(EntityHelper.ModifiedBy, UserService.SYSTEM_USER);

        try {
            Application.getBestService(targetEntity).update(targetRecord);

        } finally {
            GeneralEntityServiceContextHolder.isSkipGuardOnce();
            if (forceUpdate) GeneralEntityServiceContextHolder.isAllowForceUpdateOnce();
            if (stopPropagation) GeneralEntityServiceContextHolder.isQuickMode(true);
        }

        if (operatingContext.getAction() == BizzPermission.UPDATE && this.getClass() == FieldAggregation.class) {
            this.fieldAggregationRefresh = new FieldAggregationRefresh(this, operatingContext, targetWithMatchFields);
        }

        // 聚合后回填 (v3.1, 3.9)
        String fillbackField = ((JSONObject) actionContext.getActionContent()).getString("fillbackField");
        if (fillbackField != null && MetadataHelper.checkAndWarnField(sourceEntity, fillbackField)) {
            String sql = String.format("select %s,%s from %s where %s",
                    sourceEntity.getPrimaryField().getName(), fillbackField, sourceEntity.getName(), filterSql);
            Object[][] fillbacks = Application.createQueryNoFilter(sql).array();

            for (Object[] o : fillbacks) {
                if (CommonsUtils.isSame(o[1], targetRecordId)) continue;

                // FIXME 回填仅更新，无业务规则
                Record r = EntityHelper.forUpdate((ID) o[0], UserService.SYSTEM_USER, false);
                if (sourceEntity.getField(fillbackField).getType() == FieldType.REFERENCE_LIST) {
                    r.setIDArray(fillbackField, new ID[]{targetRecordId});
                } else {
                    r.setID(fillbackField, targetRecordId);
                }
                Application.getCommonsService().getBaseService().update(r);
            }
        }

        return TriggerResult.success(Collections.singletonList(targetRecord.getPrimary()));
    }

    @Override
    public void prepare(OperatingContext operatingContext) throws TriggerException {
        if (sourceEntity != null) return;  // 已经初始化

        final JSONObject actionContent = (JSONObject) actionContext.getActionContent();

        // FIELD.ENTITY
        String[] targetFieldEntity = ((JSONObject) actionContext.getActionContent()).getString("targetEntity").split("\\.");
        sourceEntity = actionContext.getSourceEntity();
        targetEntity = MetadataHelper.getEntity(targetFieldEntity[1]);

        String followSourceField = targetFieldEntity[0];
        if (TARGET_ANY.equals(followSourceField)) {
            targetWithMatchFields = new TargetWithMatchFields();
            targetRecordId = targetWithMatchFields.match(actionContext);
            if (targetRecordId == null && actionContent.getBooleanValue("autoCreate")) {
                targetRecordId = this.aotuCreateTargetRecord39(targetWithMatchFields);
            }
            followSourceWhere = StringUtils.join(targetWithMatchFields.getQFieldsFollow(), " and ");
            return;
        }

        if (!sourceEntity.containsField(followSourceField)) {
            throw new MissingMetaExcetion(followSourceField, sourceEntity.getName());
        }

        // 找到主记录
        Object[] o = Application.getQueryFactory().uniqueNoFilter(
                actionContext.getSourceRecord(), followSourceField, followSourceField + "." + EntityHelper.CreatedBy);
        // o[1] 为空说明记录不存在
        if (o != null && o[0] != null && o[1] != null) {
            targetRecordId = (ID) o[0];
        }

        // fix: v3.1.3 清空字段值以后无法找到记录
        if (o != null && targetRecordId == null
                && operatingContext.getAction() == BizzPermission.UPDATE && this.getClass() == FieldAggregation.class) {
            ID beforeValue = operatingContext.getBeforeRecord() == null
                    ? null : operatingContext.getBeforeRecord().getID(followSourceField);
            ID afterValue = operatingContext.getAfterRecord().getID(followSourceField);
            if (beforeValue != null && afterValue == null) {
                targetRecordId = beforeValue;
            }
        }

        if (targetRecordId == null) log.warn("Cannot found [targetRecordId]: {}", operatingContext);
        this.followSourceWhere = String.format("%s = '%s'", followSourceField, targetRecordId);
    }

    /**
     * 是否与数据库中的记录一样（就无需更新了）
     *
     * @param record
     * @return
     */
    protected boolean isCurrentSame(Record record) {
        if (!ignoreSame) return false;

        Record c = QueryHelper.querySnap(record);
        return new RecordDifference(record).isSame(c, false);
    }

    /**
     * 自动创建目标记录
     *
     * @param twmf
     * @return
     */
    protected ID aotuCreateTargetRecord39(TargetWithMatchFields twmf) {
        if (twmf.getSourceRecord() == null) return null;

        Record newTargetRecord = EntityHelper.forNew(targetEntity.getEntityCode(), UserService.SYSTEM_USER);
        for (Map.Entry<String, String> e : twmf.getMatchFieldsMapping().entrySet()) {
            String sourceField = e.getKey();
            String targetField = e.getValue();

            Object val = twmf.getSourceRecord().getObjectValue(sourceField);
            if (val != null) {
                newTargetRecord.setObjectValue(targetField, val);
            }
        }

        // 不必担心必填字段，必填只是前端约束
        // 还可以通过设置字段默认值来完成必填字段的自动填写
        // 240425 需要业务规则，譬如自动编号、默认值等

        GeneralEntityServiceContextHolder.setSkipGuard(EntityHelper.UNSAVED_ID);
        try {
            Application.getBestService(targetEntity).create(newTargetRecord);
        } finally {
            GeneralEntityServiceContextHolder.isSkipGuardOnce();
        }
        return newTargetRecord.getPrimary();
    }

    // --

    /**
     * 清理触发链（在批处理时需要调用）
     *
     * @return
     */
    public static Object cleanTriggerChain() {
        Object o = TRIGGER_CHAIN.get();
        TRIGGER_CHAIN.remove();
        return o;
    }
}
