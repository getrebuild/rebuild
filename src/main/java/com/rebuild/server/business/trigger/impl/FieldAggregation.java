/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.trigger.ActionContext;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.business.trigger.TriggerAction;
import com.rebuild.server.business.trigger.TriggerException;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.bizz.privileges.PrivilegesGuardInterceptor;
import com.rebuild.server.service.query.AdvFilterParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 字段归集可能存在的问题。
 * - 目标记录可能不允许修改（如审批已完成），此时会抛出异常
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/29
 *
 * @see com.rebuild.server.business.trigger.RobotTriggerObserver
 */
public class FieldAggregation implements TriggerAction {

    private static final Log LOG = LogFactory.getLog(FieldAggregation.class);

    /**
     * 归集到自己
     */
    public static final String SOURCE_SELF = "$PRIMARY$";

    final protected ActionContext context;
    // 允许无权限更新
    final private boolean allowNoPermissionUpdate;
    // 最大触发链深度
    final private int maxTriggerDepth;

    // 此触发器可能产生连锁反应
    // 如触发器 A 调用 B，而 B 又调用了 C ... 以此类推。此处记录其深度
    private static final ThreadLocal<Integer> TRIGGER_CHAIN_DEPTH = new ThreadLocal<>();

    // 源实体
	protected Entity sourceEntity;
	// 目标实体
    protected Entity targetEntity;
	// 关联字段
    protected String followSourceField;
	// 触发记录
    protected ID targetRecordId;

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

    @Override
	public void execute(OperatingContext operatingContext) throws TriggerException {
		Integer depth = TRIGGER_CHAIN_DEPTH.get();
		if (depth == null) {
			depth = 1;
		}
		if (depth > maxTriggerDepth) {
			throw new TriggerException("Too many trigger-chain with triggers : " + depth);
		}
		
		this.prepare(operatingContext);
		if (this.targetRecordId == null) {  // 无目标记录
			return;
		}
		
		// 如果当前用户对目标记录无修改权限
		if (!allowNoPermissionUpdate
                && !Application.getSecurityManager().allow(operatingContext.getOperator(), targetRecordId, BizzPermission.UPDATE)) {
		    LOG.warn("No privileges to update record of target: " + this.targetRecordId);
		    return;
		}

		// 聚合数据过滤
        JSONObject dataFilter = ((JSONObject) context.getActionContent()).getJSONObject("dataFilter");
		String dataFilterSql = null;
		if (dataFilter != null && !dataFilter.isEmpty()) {
            dataFilterSql = new AdvFilterParser(dataFilter).toSqlWhere();
        }

        Record targetRecord = EntityHelper.forUpdate(targetRecordId, UserService.SYSTEM_USER, false);
		buildTargetRecord(targetRecord, dataFilterSql);

		// 不含 ID
		if (targetRecord.getAvailableFields().size() > 1) {
			if (allowNoPermissionUpdate) {
				PrivilegesGuardInterceptor.setNoPermissionPassOnce(targetRecordId);
			}

			// 会关联触发下一触发器（如有）
			TRIGGER_CHAIN_DEPTH.set(depth + 1);
			Application.getService(targetEntity.getEntityCode()).update(targetRecord);
		}
	}

    /**
     * @param record
     * @param dataFilterSql
     */
	protected void buildTargetRecord(Record record, String dataFilterSql) {
        JSONArray items = ((JSONObject) context.getActionContent()).getJSONArray("items");
        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            String targetField = item.getString("targetField");
            if (!MetadataHelper.checkAndWarnField(targetEntity, targetField)) {
                continue;
            }

            Object evalValue = new AggregationEvaluator(item, sourceEntity, followSourceField, dataFilterSql)
                    .eval(targetRecordId);
            if (evalValue == null) {
                continue;
            }

            DisplayType dt = EasyMeta.getDisplayType(targetEntity.getField(targetField));
            if (dt == DisplayType.NUMBER) {
                record.setLong(targetField, ObjectUtils.toLong(evalValue));
            } else if (dt == DisplayType.DECIMAL) {
                record.setDouble(targetField, ObjectUtils.toDouble(evalValue));
            }
        }
    }

	@Override
	public void prepare(OperatingContext operatingContext) throws TriggerException {
		if (sourceEntity != null) {  // 已经初始化
			return;
		}
		
		// FIELD.ENTITY
		String[] targetFieldEntity = ((JSONObject) context.getActionContent()).getString("targetEntity").split("\\.");
		if (!MetadataHelper.containsEntity(targetFieldEntity[1])) {
			return;
		}

		this.sourceEntity = context.getSourceEntity();
		this.targetEntity = MetadataHelper.getEntity(targetFieldEntity[1]);

		// 自己
		if (SOURCE_SELF.equalsIgnoreCase(targetFieldEntity[0])) {
			this.followSourceField = sourceEntity.getPrimaryField().getName();
			this.targetRecordId = context.getSourceRecord();
		} else {
			this.followSourceField = targetFieldEntity[0];
			if (!sourceEntity.containsField(followSourceField)) {
				return;
			}

			// 找到主记录
			Object[] o = Application.getQueryFactory().uniqueNoFilter(
					context.getSourceRecord(), followSourceField, followSourceField + "." + EntityHelper.CreatedBy);
			// o[1] 为空说明记录不存在
			if (o != null && o[0] != null && o[1] != null) {
				this.targetRecordId = (ID) o[0];
			}
		}
	}

	@Override
	public void clean() {
		TRIGGER_CHAIN_DEPTH.remove();
	}
}
