/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.trigger;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

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
            entities.add(new String[] { refEntity.getName(), entityLabel, refField.getName() });
        }

        FieldAggregationController.sortEntities(entities, null);

        // 分组字段
        List<String[]> groupFields = new ArrayList<>();
        // 聚合字段
        List<String[]> sourceFields = new ArrayList<>();

        for (Field field : MetadataSorter.sortFields(sourceEntity)) {
            DisplayType dt = EasyMetaFactory.getDisplayType(field);

            if (dt == DisplayType.DATE || dt == DisplayType.TEXT
                    || dt == DisplayType.PICKLIST || dt == DisplayType.CLASSIFICATION
                    || dt == DisplayType.REFERENCE || dt == DisplayType.BOOL) {
                groupFields.add(FieldAggregationController.buildField(field));
            }

            if (!FieldAggregationController.isFilterTargetField(field)) {
                sourceFields.add(FieldAggregationController.buildField(field));
            }
        }

        return JSONUtils.toJSONObject(
                new String[] { "targetEntities", "groupFields", "sourceFields" },
                new Object[] { entities, groupFields, sourceFields });
    }

    @RequestMapping("group-aggregation-fields")
    public JSON getTargetFields(@EntityParam(name = "target") Entity targetEntity) {
        // 目标字段
        List<String[]> targetFields = new ArrayList<>();

        for (Field field : MetadataSorter.sortFields(targetEntity)) {
            DisplayType dt = EasyMetaFactory.getDisplayType(field);

            if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
                targetFields.add(FieldAggregationController.buildField(field));
            }
        }

        return JSONUtils.toJSONObject(
                new String[] { "targetFields" },
                new Object[] { targetFields });
    }
}
