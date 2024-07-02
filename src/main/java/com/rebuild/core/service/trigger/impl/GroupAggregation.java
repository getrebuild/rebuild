/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.privileges.PrivilegesGuardContextHolder;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerException;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分组聚合，场景 N[+N]>1
 *
 * @author devezhao
 * @since 2021/6/28
 */
@Slf4j
public class GroupAggregation extends FieldAggregation {

    private GroupAggregationRefresh groupAggregationRefresh;

    public GroupAggregation(ActionContext context) {
        this(context, Boolean.FALSE);
    }

    // ignoreSame 本触发器有回填，忽略同值会导致无法回填
    public GroupAggregation(ActionContext context, boolean ignoreSame) {
        super(context, ignoreSame);
    }

    @Override
    public ActionType getType() {
        return ActionType.GROUPAGGREGATION;
    }

    @Override
    public void clean() {
        super.clean();

        if (groupAggregationRefresh != null) {
            log.info("Clear after refresh : {}", groupAggregationRefresh);
            groupAggregationRefresh.refresh();
            groupAggregationRefresh = null;
        }
    }

    @Override
    public Object execute(OperatingContext operatingContext) throws TriggerException {
        return super.execute(operatingContext);
    }

    @Override
    public void prepare(OperatingContext operatingContext) throws TriggerException {
        if (sourceEntity != null) return;  // 已经初始化

        final JSONObject actionContent = (JSONObject) actionContext.getActionContent();

        sourceEntity = actionContext.getSourceEntity();
        targetEntity = MetadataHelper.getEntity(actionContent.getString("targetEntity"));

        // 0.分组字段关联 <Source, Target>

        Map<String, String> groupFieldsMapping = new HashMap<>();
        for (Object o : actionContent.getJSONArray("groupFields")) {
            JSONObject item = (JSONObject) o;
            String sourceField = item.getString("sourceField");
            String targetField = item.getString("targetField");

            if (MetadataHelper.getLastJoinField(sourceEntity, sourceField) == null) {
                throw new MissingMetaExcetion(sourceField, sourceEntity.getName());
            }
            if (!targetEntity.containsField(targetField)) {
                throw new MissingMetaExcetion(targetField, targetEntity.getName());
            }
            groupFieldsMapping.put(sourceField, targetField);
        }

        if (groupFieldsMapping.isEmpty()) {
            log.warn("No group-fields specified");
            return;
        }

        // 1.源记录数据

        String sql = String.format("select %s from %s where %s = ?",
                StringUtils.join(groupFieldsMapping.keySet().iterator(), ","),
                sourceEntity.getName(), sourceEntity.getPrimaryField().getName());

        final Record sourceRecord = Application.createQueryNoFilter(sql)
                .setParameter(1, actionContext.getSourceRecord())
                .record();

        // 2.找到目标记录

        List<String> qFields = new ArrayList<>();
        List<String> qFieldsFollow = new ArrayList<>();
        List<String[]> qFieldsRefresh = new ArrayList<>();
        boolean allNull = true;

        final boolean isGroupUpdate = operatingContext.getAction() == BizzPermission.UPDATE && this.getClass() == GroupAggregation.class;

        // 保存前/后值
        Map<String, Object[]> valueChanged = new HashMap<>();

        for (Map.Entry<String, String> e : groupFieldsMapping.entrySet()) {
            String sourceField = e.getKey();
            String targetField = e.getValue();
            // @see Dimension#getSqlName
            EasyField sourceFieldEasy = EasyMetaFactory.valueOf(MetadataHelper.getLastJoinField(sourceEntity, sourceField));
            EasyField targetFieldEasy = EasyMetaFactory.valueOf(targetEntity.getField(targetField));

            if (isGroupUpdate) {
                Object beforeValue = operatingContext.getBeforeRecord() == null
                        ? null : operatingContext.getBeforeRecord().getObjectValue(sourceField);
                Object afterValue = operatingContext.getAfterRecord().getObjectValue(sourceField);
                if (NullValue.isNull(beforeValue) && NullValue.isNull(afterValue)) {
                    // All null
                } else {
                    valueChanged.put(sourceField, new Object[] { beforeValue, afterValue });
                }
            }

            // fix: 3.7.1
            boolean isDateField = sourceFieldEasy.getDisplayType() == DisplayType.DATE
                    || sourceFieldEasy.getDisplayType() == DisplayType.DATETIME;
            int targetFieldLength = 0;
            String dateFormat = null;
            if (isDateField) {
                targetFieldLength = StringUtils.defaultIfBlank(
                        targetFieldEasy.getExtraAttr(EasyFieldConfigProps.DATE_FORMAT), targetFieldEasy.getDisplayType().getDefaultFormat()).length();

                if (targetFieldLength == 4) dateFormat = "%Y";
                else if (targetFieldLength == 7) dateFormat = "%Y-%m";
                else dateFormat = "%Y-%m-%d";
            }

            Object val = sourceRecord.getObjectValue(sourceField);
            if (val == null) {
                qFields.add(String.format("%s is null", targetField));
                qFieldsFollow.add(String.format("%s is null", sourceField));

                // for Refresh
                if (isDateField) {
                    sourceField = String.format("DATE_FORMAT(%s,'%s')", sourceField, dateFormat);
                    targetField = String.format("DATE_FORMAT(%s,'%s')", targetField, dateFormat);
                }

            } else {

                // 日期分组
                if (isDateField) {
                    String formatKey = sourceFieldEasy.getDisplayType() == DisplayType.DATE
                            ? EasyFieldConfigProps.DATE_FORMAT
                            : EasyFieldConfigProps.DATETIME_FORMAT;
                    int sourceFieldLength = StringUtils.defaultIfBlank(
                            sourceFieldEasy.getExtraAttr(formatKey), sourceFieldEasy.getDisplayType().getDefaultFormat()).length();

                    // 目标格式（长度）必须小于等于源格式
                    Assert.isTrue(targetFieldLength <= sourceFieldLength,
                            Language.L("日期字段格式不兼容") + String.format(" (%d,%d)", targetFieldLength, sourceFieldLength));

                    sourceField = String.format("DATE_FORMAT(%s,'%s')", sourceField, dateFormat);
                    targetField = String.format("DATE_FORMAT(%s,'%s')", targetField, dateFormat);
                    if (targetFieldLength == 4) {  // 'Y'
                        val = CalendarUtils.format("yyyy", (Date) val);
                    } else if (targetFieldLength == 7) {  // 'M'
                        val = CalendarUtils.format("yyyy-MM", (Date) val);
                    } else {  // 'D' is default
                        val = CalendarUtils.format("yyyy-MM-dd", (Date) val);
                    }
                }

                // 分类分组
                else if (sourceFieldEasy.getDisplayType() == DisplayType.CLASSIFICATION) {
                    int sourceFieldLevel = ClassificationManager.instance.getOpenLevel(sourceFieldEasy.getRawMeta());
                    int targetFieldLevel = ClassificationManager.instance.getOpenLevel(targetFieldEasy.getRawMeta());

                    // 目标等级必须小于等于源等级
                    Assert.isTrue(targetFieldLevel <= sourceFieldLevel,
                            Language.L("分类字段等级不兼容") + String.format(" (%d,%d)", targetFieldLevel, sourceFieldLevel));

                    // 需要匹配等级的值
                    if (sourceFieldLevel != targetFieldLevel) {
                        ID parent = TargetWithMatchFields.getItemWithLevel((ID) val, targetFieldLevel);
                        Assert.isTrue(parent != null, Language.L("分类字段等级不兼容"));

                        val = parent;
                        sourceRecord.setID(sourceField, (ID) val);

                        for (int i = 0; i < sourceFieldLevel - targetFieldLevel; i++) {
                            //noinspection StringConcatenationInLoop
                            sourceField += ".parent";
                        }
                    }
                }

                qFields.add(String.format("%s = '%s'", targetField, CommonsUtils.escapeSql(val)));
                qFieldsFollow.add(String.format("%s = '%s'", sourceField, CommonsUtils.escapeSql(val)));
                allNull = false;
            }

            qFieldsRefresh.add(new String[] { targetField, sourceField, val == null ? null : val.toString() });
        }

        if (allNull) {
            if (valueChanged.isEmpty()) {
                log.warn("All values of group-fields are null, ignored");
            } else {
                // 如果分组字段值全空将会触发全量更新
                this.groupAggregationRefresh = new GroupAggregationRefresh(this, qFieldsRefresh, operatingContext.getFixedRecordId());
            }
            return;
        }

        this.followSourceWhere = StringUtils.join(qFieldsFollow.iterator(), " and ");

        if (isGroupUpdate) {
            this.groupAggregationRefresh = new GroupAggregationRefresh(this, qFieldsRefresh, operatingContext.getFixedRecordId());
        }

        sql = String.format("select %s from %s where ( %s )",
                targetEntity.getPrimaryField().getName(), targetEntity.getName(),
                StringUtils.join(qFields.iterator(), " and "));

        Object[] targetRecord = Application.createQueryNoFilter(sql).unique();
        if (targetRecord != null) {
            targetRecordId = (ID) targetRecord[0];
            return;
        }

        // 是否自动创建记录
        if (!actionContent.getBooleanValue("autoCreate")) {
            this.groupAggregationRefresh = null;  // 无更新也就无需刷新
            return;
        }

        // 不必担心必填字段，必填只是前端约束
        // 还可以通过设置字段默认值来完成必填字段的自动填写
        // 0425 需要业务规则，譬如自动编号、默认值等

        Record newTargetRecord = EntityHelper.forNew(targetEntity.getEntityCode(), UserService.SYSTEM_USER);
        for (Map.Entry<String, String> e : groupFieldsMapping.entrySet()) {
            String sourceField = e.getKey();
            String targetField = e.getValue();

            Object val = sourceRecord.getObjectValue(sourceField);
            if (val != null) {
                newTargetRecord.setObjectValue(targetField, val);
            }
        }

        PrivilegesGuardContextHolder.setSkipGuard(EntityHelper.UNSAVED_ID);

        try {
            Application.getBestService(targetEntity).create(newTargetRecord);
        } finally {
            PrivilegesGuardContextHolder.getSkipGuardOnce();
        }

        targetRecordId = newTargetRecord.getPrimary();
    }
}
