/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.trigger;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.core.service.trigger.impl.FieldWriteback;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.general.BatchUpdateController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.Collator;
import java.util.*;

/**
 * @author devezhao
 * @see FieldAggregationController
 * @since 2020/2/7
 */
@RestController
@RequestMapping("/admin/robot/trigger/")
public class FieldWritebackController extends BaseController {

    @RequestMapping("field-writeback-entities")
    public List<String[]> getTargetEntities(@EntityParam(name = "source") Entity sourceEntity) {
        List<String[]> entities = new ArrayList<>();
        // 谁引用了我
        for (Field refto : MetadataHelper.getReferenceToFields(sourceEntity)) {
            String entityLabel = String.format("%s (%s)",
                    EasyMetaFactory.getLabel(refto.getOwnEntity()), EasyMetaFactory.getLabel(refto));
            entities.add(new String[] {
                    refto.getOwnEntity().getName(), entityLabel, refto.getName() });
        }

        FieldAggregationController.sortEntities(entities, sourceEntity);
        return entities;
    }

    @RequestMapping("field-writeback-fields")
    public JSON getTargetFields(@EntityParam(name = "source") Entity sourceEntity, HttpServletRequest request) {
        String target = getParameter(request, "target");
        Entity targetEntity = StringUtils.isBlank(target) ? null : MetadataHelper.getEntity(target);

        JSONArray sourceFields = new JSONArray();
        JSONArray targetFields = new JSONArray();

        // 源字段

        // 本实体
        sourceFields.add(EasyMetaFactory.toJSON(sourceEntity.getPrimaryField()));
        for (Field field : MetadataSorter.sortFields(sourceEntity)) {
            EasyField easyField = EasyMetaFactory.valueOf(field);
            if (easyField.getDisplayType() == DisplayType.BARCODE) continue;

            sourceFields.add(easyField.toJSON());
        }

        // 关联实体的
        for (Field fieldRef : MetadataSorter.sortFields(sourceEntity, DisplayType.REFERENCE)) {
            // FIXME 是否过滤系统级引用实体 ???
            if (MetadataHelper.isCommonsField(fieldRef)) continue;

            Entity refEntity = fieldRef.getReferenceEntity();
            if (refEntity.getEntityCode() == EntityHelper.RobotApprovalConfig) continue;

            String fieldRefName = fieldRef.getName() + ".";
            String fieldRefLabel = EasyMetaFactory.getLabel(fieldRef) + ".";

            for (Field field : MetadataSorter.sortFields(refEntity)) {
                EasyField easyField = EasyMetaFactory.valueOf(field);
                if (easyField.getDisplayType() == DisplayType.BARCODE) continue;

                JSONObject subField = (JSONObject) easyField.toJSON();
                subField.put("name", fieldRefName + subField.getString("name"));
                subField.put("label", fieldRefLabel + subField.getString("label"));
                sourceFields.add(subField);
            }
        }

        // 目标字段

        if (targetEntity != null) {
            for (Field field : MetadataSorter.sortFields(targetEntity)) {
                EasyField easyField = EasyMetaFactory.valueOf(field);
                DisplayType dt = easyField.getDisplayType();
                if (dt == DisplayType.SERIES || dt == DisplayType.BARCODE
                        || dt == DisplayType.ANYREFERENCE || easyField.isBuiltin()) {
                    continue;
                }

                targetFields.add(BatchUpdateController.buildField(easyField));
            }
        }

        // 审批流程启用
        boolean hadApproval = targetEntity != null
                && RobotApprovalManager.instance.hadApproval(targetEntity, null) != null;

        return JSONUtils.toJSONObject(
                new String[]{"source", "target", "hadApproval"},
                new Object[]{sourceFields, targetFields, hadApproval});
    }
}
