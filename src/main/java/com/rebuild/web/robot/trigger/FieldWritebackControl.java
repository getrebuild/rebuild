/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.trigger;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao
 * @see FieldAggregationControl
 * @since 2020/2/7
 */
@Controller
@RequestMapping("/admin/robot/trigger/")
public class FieldWritebackControl extends BaseController {

    @RequestMapping("field-writeback-fields")
    public void getTargetField(HttpServletRequest request, HttpServletResponse response) {
        Entity sourceEntity = MetadataHelper.getEntity(getParameterNotNull(request, "source"));
        String target = getParameter(request, "target");
        Entity targetEntity = StringUtils.isBlank(target) ? null : MetadataHelper.getEntity(target);

        List<String[]> sourceFields = new ArrayList<>();
        List<String[]> targetFields = new ArrayList<>();

        // 源字段

        sourceFields.add(FieldAggregationControl.buildField(sourceEntity.getPrimaryField(), true));
        for (Field field : MetadataSorter.sortFields(sourceEntity)) {
            sourceFields.add(FieldAggregationControl.buildField(field, true));
        }
        // 关联实体
        for (Field fieldRef : MetadataSorter.sortFields(sourceEntity, DisplayType.REFERENCE)) {
            Entity refEntity = fieldRef.getReferenceEntity();
            if (refEntity.getEntityCode() == EntityHelper.RobotApprovalConfig) {
                continue;
            }

            String fieldRefName = fieldRef.getName() + ".";
            String fieldRefLabel = EasyMeta.getLabel(fieldRef) + ".";
            for (Field field : MetadataSorter.sortFields(refEntity)) {
                String[] build = FieldAggregationControl.buildField(field, true);
                build[0] = fieldRefName + build[0];
                build[1] = fieldRefLabel + build[1];
                sourceFields.add(build);
            }
        }

        // 目标字段

        if (targetEntity != null) {
            for (Field field : MetadataSorter.sortFields(targetEntity)) {
                EasyMeta easyField = EasyMeta.valueOf(field);
                DisplayType dt = easyField.getDisplayType();
                if (dt == DisplayType.SERIES || dt == DisplayType.MULTISELECT || easyField.isBuiltin()) {
                    continue;
                }
                targetFields.add(FieldAggregationControl.buildField(field, true));
            }
        }

        // 审批流程启用
        boolean hadApproval = targetEntity != null && RobotApprovalManager.instance.hadApproval(targetEntity, null) != null;

        JSON data = JSONUtils.toJSONObject(
                new String[]{"source", "target", "hadApproval"},
                new Object[]{
                        sourceFields.toArray(new String[sourceFields.size()][]),
                        targetFields.toArray(new String[targetFields.size()][]),
                        hadApproval});
        writeSuccess(response, data);
    }
}
