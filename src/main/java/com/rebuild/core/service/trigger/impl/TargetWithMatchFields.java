/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通过字段匹配记录（而非通过引用关联）
 *
 * @author devezhao
 * @since 2023/10/25
 */
@Slf4j
public class TargetWithMatchFields {

    private Entity sourceEntity;

    @Getter
    private List<String> qFieldsFollow;
    @Getter
    private List<String[]> qFieldsRefresh;
    @Getter
    private Object targetRecordId;

    protected TargetWithMatchFields() {
        super();
    }

    /**
     * @param actionContext
     * @return
     */
    public ID match(ActionContext actionContext) {
        return (ID) match(actionContext, false);
    }

    /**
     * @param actionContext
     * @return
     */
    public ID[] matchMulti(ActionContext actionContext) {
        Object o = match(actionContext, true);
        return o == null ? new ID[0] : (ID[]) o;
    }

    /**
     * @param actionContext
     * @param m
     * @return
     * @see GroupAggregation#prepare(OperatingContext)
     */
    private Object match(ActionContext actionContext, boolean m) {
        if (sourceEntity != null) return targetRecordId;  // 已做匹配

        final JSONObject actionContent = (JSONObject) actionContext.getActionContent();
        sourceEntity = actionContext.getSourceEntity();
        Entity targetEntity = MetadataHelper.getEntity(actionContent.getString("targetEntity").split("\\.")[1]);

        // 0.字段关联 <Source, Target>

        Map<String, String> matchFieldsMapping = new HashMap<>();

        JSONArray matchFields = actionContent.getJSONArray("targetEntityMatchFields");
        if (matchFields == null) matchFields = actionContent.getJSONArray("groupFields");

        for (Object o : matchFields) {
            JSONObject item = (JSONObject) o;
            String sourceField = item.getString("sourceField");
            String targetField = item.getString("targetField");

            if (MetadataHelper.getLastJoinField(sourceEntity, sourceField) == null) {
                throw new MissingMetaExcetion(sourceField, sourceEntity.getName());
            }
            if (!targetEntity.containsField(targetField)) {
                throw new MissingMetaExcetion(targetField, targetEntity.getName());
            }
            matchFieldsMapping.put(sourceField, targetField);
        }

        if (matchFieldsMapping.isEmpty()) {
            log.warn("No match-fields specified");
            return null;
        }

        // 1.源记录数据

        String aSql = String.format("select %s from %s where %s = ?",
                StringUtils.join(matchFieldsMapping.keySet().iterator(), ","),
                sourceEntity.getName(), sourceEntity.getPrimaryField().getName());

        final Record sourceRecord = Application.createQueryNoFilter(aSql)
                .setParameter(1, actionContext.getSourceRecord())
                .record();

        // 2.找到目标记录

        boolean allNull = true;
        List<String> qFields = new ArrayList<>();
        qFieldsFollow = new ArrayList<>();
        qFieldsRefresh = new ArrayList<>();

        for (Map.Entry<String, String> e : matchFieldsMapping.entrySet()) {
            String sourceField = e.getKey();
            String targetField = e.getValue();
            // @see Dimension#getSqlName
            EasyField sourceFieldEasy = EasyMetaFactory.valueOf(MetadataHelper.getLastJoinField(sourceEntity, sourceField));
            EasyField targetFieldEasy = EasyMetaFactory.valueOf(targetEntity.getField(targetField));

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
                        ID parent = getItemWithLevel((ID) val, targetFieldLevel);
                        Assert.isTrue(parent != null, Language.L("分类字段等级不兼容"));

                        val = parent;
                        sourceRecord.setID(sourceField, (ID) val);

                        for (int i = 0; i < sourceFieldLevel - targetFieldLevel; i++) {
                            //noinspection StringConcatenationInLoop
                            sourceField += ".parent";
                        }
                    }
                }

                val = CommonsUtils.escapeSql(val);
                qFields.add(String.format("%s = '%s'", targetField, val));
                qFieldsFollow.add(String.format("%s = '%s'", sourceField, val));
                allNull = false;
            }

            qFieldsRefresh.add(new String[] { targetField, sourceField, val == null ? null : val.toString() });
        }

        if (allNull) {
            log.warn("All values of match-fields are null, ignored");
            return null;
        }

        aSql = String.format("select %s from %s where ( %s )",
                targetEntity.getPrimaryField().getName(), targetEntity.getName(),
                StringUtils.join(qFields.iterator(), " and "));

        // 多个 1:N
        if (m) {
            Object[][] array = Application.createQueryNoFilter(aSql).array();
            List<ID> targetRecordIds = new ArrayList<>();
            for (Object[] o : array) targetRecordIds.add((ID) o[0]);

            targetRecordId = targetRecordIds.toArray(new ID[0]);
            return targetRecordId;
        }

        // 单个
        Object[] targetRecord = Application.createQueryNoFilter(aSql).unique();
        targetRecordId = targetRecord == null ? null : (ID) targetRecord[0];
        return targetRecordId;
    }

    /**
     * 分类字段级别
     *
     * @param itemId
     * @param specLevel
     * @return
     */
    static ID getItemWithLevel(ID itemId, int specLevel) {
        ID current = itemId;
        for (int i = 0; i < 4; i++) {
            Object[] o = Application.createQueryNoFilter(
                    "select level,parent from ClassificationData where itemId = ?")
                    .setParameter(1, current)
                    .unique();

            if (o == null) break;
            if ((int) o[0] < specLevel) break;

            if ((int) o[0] == specLevel) return current;
            else current = (ID) o[1];
        }
        return null;
    }
}
