/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.trigger;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.api.RespBody;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.general.MetaFormatter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao
 * @since 2019/05/25
 */
@RestController
@RequestMapping("/admin/robot/trigger/")
public class FieldAggregationController extends BaseController {

    @RequestMapping("field-aggregation-entities")
    public RespBody getTargetEntities(
            @EntityParam(name = "source") Entity sourceEntity, HttpServletRequest request) {
        List<Object[]> entities = new ArrayList<>();

        // 1. 我引用了谁

        for (Field refFrom : MetadataSorter.sortFields(sourceEntity, DisplayType.REFERENCE)) {
            if (MetadataHelper.isApprovalField(refFrom.getName())) continue;

            Entity refEntity = refFrom.getReferenceEntity();
            String entityLabel = String.format("%s (%s)",
                    EasyMetaFactory.getLabel(refEntity), EasyMetaFactory.getLabel(refFrom));
            entities.add(new String[] { refEntity.getName(), entityLabel, refFrom.getName() });
        }

        // v35 字段匹配
        if (getBoolParameter(request, "matchfields")) {
            List<Object[]> temp = new ArrayList<>();
            for (Entity entity : MetadataSorter.sortEntities(null, false, true)) {
                temp.add(new String[]{entity.getName(), EasyMetaFactory.getLabel(entity), "$"});
            }

            MetadataSorter.sortEntities(temp, null);
            entities.addAll(temp);
        }

        // v3.0 字段聚合无需自己
        MetadataSorter.sortEntities(entities, null);

        return RespBody.ok(entities);
    }

    @RequestMapping("field-aggregation-fields")
    public JSON getTargetFields(@EntityParam(name = "source") Entity sourceEntity,
                                HttpServletRequest request) {
        String target = getParameter(request, "target");
        Entity targetEntity = StringUtils.isBlank(target) ? null : MetadataHelper.getEntity(target);

        // 源字段

        JSONArray sourceFields = MetaFormatter.buildFieldsWithRefs(sourceEntity, 3, true, field -> {
            if (field instanceof EasyField) {
                EasyField easyField = (EasyField) field;
                return !easyField.isQueryable() || easyField.getDisplayType() == DisplayType.BARCODE;
            }
            return MetadataHelper.isApprovalField(field.getName());
        });

        JSONArray tmp = new JSONArray();
        tmp.add(EasyMetaFactory.toJSON(sourceEntity.getPrimaryField()));
        tmp.addAll(sourceFields);
        sourceFields = tmp;

        // 目标字段

        JSONArray targetFields = new JSONArray();
        if (targetEntity != null) {
            targetFields = MetaFormatter.buildFieldsWithRefs(targetEntity, 1, true, field -> {
                EasyField e = (EasyField) field;
                return e.getDisplayType() == DisplayType.SERIES || e.getDisplayType() == DisplayType.BARCODE
                        || e.isBuiltin();
            });
        }

        // 审批流程启用
        boolean hadApproval = targetEntity != null && RobotApprovalManager.instance.hadApproval(
                ObjectUtils.getIfNull(targetEntity.getMainEntity(), targetEntity), null) != null;

        return JSONUtils.toJSONObject(
                new String[] { "source", "target", "hadApproval", "target4Group" },
                new Object[] { sourceFields, targetFields, hadApproval, targetFields });
    }
}
