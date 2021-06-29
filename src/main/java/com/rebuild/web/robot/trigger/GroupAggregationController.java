/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.trigger;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static com.rebuild.web.robot.trigger.FieldAggregationController.*;

/**
 * @author devezhao
 * @since 2021/6/28
 */
@RestController
@RequestMapping("/admin/robot/trigger/")
public class GroupAggregationController extends BaseController {

    @RequestMapping("group-aggregation-entities")
    public JSON getTargetEntities(@EntityParam(name = "source") Entity sourceEntity) {
        // 目标实体
        List<String[]> entities = new ArrayList<>();
        // 我引用了谁
        for (Field refField : MetadataSorter.sortFields(sourceEntity, DisplayType.REFERENCE)) {
            if (MetadataHelper.isApprovalField(refField.getName())) continue;

            Entity refEntity = refField.getReferenceEntity();
            // 过滤非业务实体
            if (!MetadataHelper.isBusinessEntity(refEntity)) continue;

            String entityLabel = String.format("%s (%s)",
                    EasyMetaFactory.getLabel(refEntity), EasyMetaFactory.getLabel(refField));
            entities.add(new String[]{refEntity.getName(), entityLabel, refField.getName()});
        }

        sortEntities(entities, null);

        // 分组源字段
        List<String[]> sourceGroupFields = new ArrayList<>();
        // 聚合源字段
        List<String[]> sourceFields = new ArrayList<>();

        for (Field field : MetadataSorter.sortFields(sourceEntity)) {
            EasyField easyField = EasyMetaFactory.valueOf(field);
            if (isAllowGroupField(easyField)) {
                sourceGroupFields.add(buildField(field));
            }

            if (isAllowSourceField(field)) {
                sourceFields.add(buildField(field));
            }
        }

        return JSONUtils.toJSONObject(
                new String[] { "targetEntities", "sourceGroupFields", "sourceFields" },
                new Object[] { entities, sourceGroupFields, sourceFields });
    }

    @RequestMapping("group-aggregation-fields")
    public JSON getTargetFields(@EntityParam(name = "target") Entity targetEntity) {
        // 分组目标字段
        List<String[]> targetGroupFields = new ArrayList<>();
        // 目标字段
        List<String[]> targetFields = new ArrayList<>();

        for (Field field : MetadataSorter.sortFields(targetEntity)) {
            EasyField easyField = EasyMetaFactory.valueOf(field);
            if (isAllowGroupField(easyField)) {
                targetGroupFields.add(buildField(field));
            }

            DisplayType dt = easyField.getDisplayType();
            if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
                targetFields.add(buildField(field));
            }
        }

        return JSONUtils.toJSONObject(
                new String[] { "targetGroupFields", "targetFields" },
                new Object[] { targetGroupFields, targetFields });
    }

    private boolean isAllowGroupField(EasyField field) {
        DisplayType dt = field.getDisplayType();
        return (dt == DisplayType.DATE || dt == DisplayType.TEXT
                || dt == DisplayType.PICKLIST || dt == DisplayType.CLASSIFICATION
                || dt == DisplayType.REFERENCE || dt == DisplayType.BOOL);
    }
}
