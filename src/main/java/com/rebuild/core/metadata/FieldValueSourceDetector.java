/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.trigger.aviator.AviatorUtils;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段值来源
 *
 * @author Zixin (RB)
 * @see EasyMetaFactory
 * @since 10/21/2025
 */
public class FieldValueSourceDetector {

    /**
     * @param field
     * @return
     */
    public List<Object> detect(Field field) {
        List<Object> res = detectTriggers(field);
        res.addAll(detectAutoFillin(field));
        return res;
    }

    /**
     * 从触发器
     *
     * @param field
     * @return
     */
    protected List<Object> detectTriggers(Field field) {
        Entity entity = field.getOwnEntity();
        Object[][] array = Application.createQueryNoFilter(
                "select configId,actionType,actionContent,belongEntity from RobotTriggerConfig where when > 0 and actionContent is not null and isDisabled <> 'T'")
                .array();

        List<Object> res = new ArrayList<>();
        for (Object[] o : array) {
            String config = (String) o[2];
            if (!JSONUtils.wellFormat(config)) continue;

            JSONObject configJson = JSON.parseObject(config);
            String targetEntity = configJson.getString("targetEntity");
            if (StringUtils.isBlank(targetEntity)) {
                // {file:xx, entity:xx}
                JSONObject forAutoGenReport = configJson.getJSONObject("genAfterDest");
                if (forAutoGenReport != null && forAutoGenReport.containsKey("file")) {
                    Object file = detectTriggersAutoGenReport(field, configJson, (ID) o[0]);
                    if (file != null) res.add(file);
                }
            }
            if (StringUtils.isBlank(targetEntity)) continue;
            // FIELD.ENTITY
            if (targetEntity.contains(".")) targetEntity = targetEntity.split("\\.")[1];

            // 源实体
            String sourceEntity = o[3].toString();
            Entity sourceEntityMeta = MetadataHelper.getEntity(sourceEntity);

            // 聚合后回填
            String fillbackField = configJson.getString("fillbackField");
            Field lastJoinField;
            if (fillbackField != null && (lastJoinField = MetadataHelper.getLastJoinField(sourceEntityMeta, fillbackField)) != null) {
                if (lastJoinField.equals(field)) {
                    Field idField = MetadataHelper.getField(targetEntity, targetEntity + "Id");
                    fillbackField = String.format("[%s.%s](/admin/entity/%s/field/%s)",
                            Language.L(idField.getOwnEntity()), Language.L(idField), targetEntity, idField.getName());

                    String desc = String.format("触发 [%s](/admin/robot/trigger/%s) 时从 %s",
                            FieldValueHelper.getLabelNotry((ID) o[0]), o[0], fillbackField);
                    res.add(new String[]{"RobotTriggerConfig", desc});
                }
            }

            // 目标不一致
            if (!targetEntity.equalsIgnoreCase(entity.getName())) continue;

            JSONArray sourceAndTargetItems = configJson.getJSONArray("items");
            if (CollectionUtils.isEmpty(sourceAndTargetItems)) continue;

            for (Object item : sourceAndTargetItems) {
                JSONObject itemJson = (JSONObject) item;
                if (itemJson.getString("targetField").equals(field.getName())) {
                    String sourceField = StringUtils.defaultIfEmpty(
                            itemJson.getString("sourceField"), itemJson.getString("sourceFormula"));

                    if (sourceField.startsWith(AviatorUtils.CODE_PREFIX)) {
                        sourceField = "[计算公式]";
                    } else if ((lastJoinField = MetadataHelper.getLastJoinField(entity, sourceField)) != null) {
                        sourceField = Language.L(entity, sourceField);
                        sourceField = String.format("[%s](/admin/entity/%s/field/%s)",
                                sourceField, lastJoinField.getOwnEntity().getName(), lastJoinField.getName());
                    } else {
                        sourceField = "[" + sourceField.toUpperCase() + "]";
                    }

                    String desc = String.format("触发 [%s](/admin/robot/trigger/%s) 时从 %s",
                            FieldValueHelper.getLabelNotry((ID) o[0]), o[0], sourceField);
                    res.add(new String[]{"RobotTriggerConfig", desc});
                }
            }
        }
        return res;
    }

    // for 自动报表
    private Object detectTriggersAutoGenReport(Field field, JSONObject forAutoGenReport, ID triggerId) {
        JSONObject genAfterDest = forAutoGenReport.getJSONObject("genAfterDest");
        String fileField = genAfterDest.getString("file");
        String fileEntity = genAfterDest.getString("entity");
        if (fileField == null) return null;

        boolean s = fileEntity == null && fileField.equals(field.getName());
        if (!s && fileEntity != null) s = MetadataHelper.getField(fileEntity, fileField).equals(field);

        if (s) {
            String reportId = forAutoGenReport.getString("useTemplate");
            ID reportId2 = ID.valueOf(reportId);
            String desc = String.format("触发 [%s](/admin/robot/trigger/%s) 时从 [%s](/admin/data/report-templates?gs=%s)",
                    FieldValueHelper.getLabelNotry(triggerId), triggerId,
                    "[" + Language.L("报表") + "] " + FieldValueHelper.getLabelNotry(reportId2), reportId2);
            return new String[]{"RobotTriggerConfig", desc};
        }
        return null;
    }

    /**
     * 从表单回填
     *
     * @param field
     * @return
     */
    protected List<Object> detectAutoFillin(Field field) {
        Entity entity = field.getOwnEntity();
        Object[][] array = Application.createQueryNoFilter(
                "select belongField,sourceField from AutoFillinConfig where belongEntity = ? and targetField = ?")
                .setParameter(1, entity.getName())
                .setParameter(2, field.getName())
                .array();

        List<Object> res = new ArrayList<>();
        for (Object[] o : array) {
            Field sourceRefField = entity.getField((String) o[0]);
            Entity sourceRefEntity = sourceRefField.getReferenceEntity();
            Field sourceValueField = sourceRefEntity.getField((String) o[1]);

            String desc = String.format(
                    "选择 [%s](/admin/entity/%s/field/%s/auto-fillin) 时从 [%s](/admin/entity/%s/field/%s)",
                    Language.L(sourceRefField), entity.getName(), sourceRefField.getName(),
                    Language.L(sourceRefEntity, (String) o[1]), sourceRefEntity.getName(), sourceValueField.getName());
            res.add(new String[]{"AutoFillinConfig", desc});
        }
        return res;
    }
}
