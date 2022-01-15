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
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static com.rebuild.web.robot.trigger.FieldAggregationController.buildField;
import static com.rebuild.web.robot.trigger.FieldAggregationController.isAllowSourceField;

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
        // 任意
        for (Entity entity : MetadataSorter.sortEntities(null, false, true)) {
            if (entity.equals(sourceEntity)) continue;
            entities.add(new String[] { entity.getName(), EasyMetaFactory.getLabel(entity) });
        }

        // 源-分组字段
        List<String[]> sourceGroupFields = new ArrayList<>();
        // 源-聚合字段
        List<String[]> sourceFields = new ArrayList<>();

        for (Field field : MetadataSorter.sortFields(sourceEntity)) {
            EasyField easyField = EasyMetaFactory.valueOf(field);
            String[] build = buildIfGroupField(easyField);
            if (build != null) sourceGroupFields.add(build);

            if (isAllowSourceField(field)) {
                sourceFields.add(buildField(field));
            }
        }
        // 明细实体则包括主实体
        if (sourceEntity.getMainEntity() != null) {
            String prefixName = MetadataHelper.getDetailToMainField(sourceEntity).getName() + ".";
            String prefixLabel = EasyMetaFactory.getLabel(sourceEntity.getMainEntity()) + ".";

            for (Field field : MetadataSorter.sortFields(sourceEntity.getMainEntity())) {
                EasyField easyField = EasyMetaFactory.valueOf(field);
                String[] build = buildIfGroupField(easyField);
                if (build != null) {
                    build[0] = prefixName + build[0];
                    build[1] = prefixLabel + build[1];
                    sourceGroupFields.add(build);
                }
            }
        }

        return JSONUtils.toJSONObject(
                new String[] { "targetEntities", "sourceGroupFields", "sourceFields" },
                new Object[] { entities, sourceGroupFields, sourceFields });
    }

    @RequestMapping("group-aggregation-fields")
    public JSON getTargetFields(@EntityParam(name = "target") Entity targetEntity) {
        // 目标-分组字段
        List<String[]> targetGroupFields = new ArrayList<>();
        // 目标字段
        List<String[]> targetFields = new ArrayList<>();

        for (Field field : MetadataSorter.sortFields(targetEntity)) {
            EasyField easyField = EasyMetaFactory.valueOf(field);
            String[] build = buildIfGroupField(easyField);
            if (build != null) targetGroupFields.add(build);

            DisplayType dt = easyField.getDisplayType();
            if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
                targetFields.add(buildField(field));
            }
        }

        return JSONUtils.toJSONObject(
                new String[] { "targetGroupFields", "targetFields" },
                new Object[] { targetGroupFields, targetFields });
    }

    private String[] buildIfGroupField(EasyField field) {
        DisplayType dt = field.getDisplayType();

        // 分组字段类型的支持
        boolean allow = dt == DisplayType.TEXT || dt == DisplayType.DATE
                || dt == DisplayType.CLASSIFICATION || dt == DisplayType.REFERENCE;
        if (!allow) return null;
        
        String[] build = buildField(field.getRawMeta());
        if (dt == DisplayType.CLASSIFICATION) {
            build[2] += ":" + field.getExtraAttr(EasyFieldConfigProps.CLASSIFICATION_USE);
        } else if (dt == DisplayType.REFERENCE) {
            build[2] += ":" + field.getRawMeta().getReferenceEntity().getName();
        }
        return build;
    }
}
