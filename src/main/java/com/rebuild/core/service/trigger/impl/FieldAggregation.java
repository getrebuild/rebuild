/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.PrivilegesGuardContextHolder;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.ServiceSpec;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerException;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段聚合。问题：
 * - 目标记录可能不允许修改（如审批已完成），此时会抛出异常
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/29
 */
@Slf4j
public class FieldAggregation implements TriggerAction {

    /**
     * 更新自己
     */
    public static final String SOURCE_SELF = "$PRIMARY$";

    final protected ActionContext context;

    // 允许无权限更新
    final protected boolean allowNoPermissionUpdate;
    // 最大触发链深度
    final protected int maxTriggerDepth;
    // 此触发器可能产生连锁反应
    // 如触发器 A 调用 B，而 B 又调用了 C ... 以此类推。此处记录其深度
    protected static final ThreadLocal<List<ID>> TRIGGER_CHAIN_DEPTH = new ThreadLocal<>();

    // 源实体
    protected Entity sourceEntity;
    // 目标实体
    protected Entity targetEntity;

    // 目标记录
    protected ID targetRecordId;
    // 关联字段条件
    protected String followSourceWhere;

    /**
     * @param context
     */
    public FieldAggregation(ActionContext context) {
        this(context, Boolean.TRUE, 5);
    }

    /**
     * @param context
     * @param allowNoPermissionUpdate
     * @param maxTriggerDepth
     */
    protected FieldAggregation(ActionContext context, boolean allowNoPermissionUpdate, int maxTriggerDepth) {
        this.context = context;
        this.allowNoPermissionUpdate = allowNoPermissionUpdate;
        this.maxTriggerDepth = maxTriggerDepth;
    }

    @Override
    public ActionType getType() {
        return ActionType.FIELDAGGREGATION;
    }

    /**
     * 检查调用链
     *
     * @return
     */
    protected List<ID> checkTriggerChain() {
        List<ID> tschain = TRIGGER_CHAIN_DEPTH.get();
        if (tschain == null) {
            tschain = new ArrayList<>();
        } else {
            ID triggerCurrent = context.getConfigId();
            log.info("Occured trigger-chain : {} > {} (current)",
                    StringUtils.join(tschain, " > "), triggerCurrent);

//            // 同一触发器不能连续触发（源实体与目标实体相同时）
//            ID lastTrigger = tschain.get(tschain.size() - 1);
//            if (triggerCurrent.equals(lastTrigger)) {
//                return null;
//            }
//            // 循环调用（如 A > B > A）
//            if (tschain.contains(triggerCurrent)) {
//                return null;
//            }

            // 在整个触发链上只触发一次
            if (tschain.contains(triggerCurrent)) {
                return null;
            }
        }

        if (tschain.size() >= maxTriggerDepth) {
            throw new TriggerException("Exceed the maximum trigger depth : " + StringUtils.join(tschain, " > "));
        }

        return tschain;
    }

    @Override
    public void execute(OperatingContext operatingContext) throws TriggerException {
        final List<ID> tschain = checkTriggerChain();
        if (tschain == null) return;

        this.prepare(operatingContext);
        if (targetRecordId == null) {
            log.warn("No target record found");
            return;
        }

        // 如果当前用户对目标记录无修改权限
        if (!allowNoPermissionUpdate
                && !Application.getPrivilegesManager().allow(operatingContext.getOperator(), targetRecordId, BizzPermission.UPDATE)) {
            log.warn("No permission to update record of target : {}", targetRecordId);
            return;
        }

        // 聚合数据过滤
        JSONObject dataFilter = ((JSONObject) context.getActionContent()).getJSONObject("dataFilter");
        String dataFilterSql = null;
        if (dataFilter != null && !dataFilter.isEmpty()) {
            dataFilterSql = new AdvFilterParser(dataFilter).toSqlWhere();
        }

        // 构建目标记录数据
        Record targetRecord = EntityHelper.forUpdate(targetRecordId, UserService.SYSTEM_USER, false);
        JSONArray items = ((JSONObject) context.getActionContent()).getJSONArray("items");
        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            String targetField = item.getString("targetField");
            if (!MetadataHelper.checkAndWarnField(targetEntity, targetField)) {
                continue;
            }

            String filterSql = followSourceWhere;
            if (dataFilterSql != null) {
                filterSql = String.format("( %s ) and ( %s )", followSourceWhere, dataFilterSql);
            }

            Object evalValue = new AggregationEvaluator(item, sourceEntity, filterSql).eval();
            if (evalValue == null) continue;

            DisplayType dt = EasyMetaFactory.getDisplayType(targetEntity.getField(targetField));
            if (dt == DisplayType.NUMBER) {
                targetRecord.setLong(targetField, CommonsUtils.toLongHalfUp(evalValue));
            } else if (dt == DisplayType.DECIMAL) {
                targetRecord.setDouble(targetField, ObjectUtils.toDouble(evalValue));
            }
        }

        // 有需要才执行
        if (targetRecord.getAvailableFields().size() > 1) {
            if (allowNoPermissionUpdate) {
                PrivilegesGuardContextHolder.setSkipGuard(targetRecordId);
            }

            // 会关联触发下一触发器（如有）
            tschain.add(context.getConfigId());
            TRIGGER_CHAIN_DEPTH.set(tschain);

            ServiceSpec useService = MetadataHelper.isBusinessEntity(targetEntity)
                    ? Application.getEntityService(targetEntity.getEntityCode())
                    : Application.getService(targetEntity.getEntityCode());

            try {
                useService.update(targetRecord);
            } finally {
                PrivilegesGuardContextHolder.getSkipGuardOnce();
            }
        }
    }

    @Override
    public void prepare(OperatingContext operatingContext) throws TriggerException {
        if (sourceEntity != null) return;  // 已经初始化

        // FIELD.ENTITY
        String[] targetFieldEntity = ((JSONObject) context.getActionContent()).getString("targetEntity").split("\\.");
        sourceEntity = context.getSourceEntity();
        targetEntity = MetadataHelper.getEntity(targetFieldEntity[1]);

        String followSourceField;

        // 自己
        if (SOURCE_SELF.equalsIgnoreCase(targetFieldEntity[0])) {
            followSourceField = sourceEntity.getPrimaryField().getName();
            targetRecordId = context.getSourceRecord();
        } else {
            followSourceField = targetFieldEntity[0];
            if (!sourceEntity.containsField(followSourceField)) {
                throw new MissingMetaExcetion(followSourceField, sourceEntity.getName());
            }

            // 找到主记录
            Object[] o = Application.getQueryFactory().uniqueNoFilter(
                    context.getSourceRecord(), followSourceField, followSourceField + "." + EntityHelper.CreatedBy);
            // o[1] 为空说明记录不存在
            if (o != null && o[0] != null && o[1] != null) {
                targetRecordId = (ID) o[0];
            }
        }

        this.followSourceWhere = String.format("%s = '%s'", followSourceField, targetRecordId);
    }

    @Override
    public void clean() {
        TRIGGER_CHAIN_DEPTH.remove();
    }
}
