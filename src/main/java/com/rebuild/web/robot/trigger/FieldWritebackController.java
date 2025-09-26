/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.trigger;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.core.service.trigger.aviator.AssertFailedException;
import com.rebuild.core.service.trigger.impl.AggregationEvaluator;
import com.rebuild.core.service.trigger.impl.FieldWriteback;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.general.MetaFormatter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author devezhao
 * @see FieldAggregationController
 * @since 2020/2/7
 */
@Slf4j
@RestController
@RequestMapping("/admin/robot/trigger/")
public class FieldWritebackController extends BaseController {

    @RequestMapping("field-writeback-entities")
    public RespBody getTargetEntities(
            @EntityParam(name = "source") Entity sourceEntity, HttpServletRequest request) {
        List<Object[]> temp = new ArrayList<>();
        Set<String> unique = new HashSet<>();

        // 1.我引用了谁 v2.7.1

        for (Field refFrom : MetadataSorter.sortFields(sourceEntity, DisplayType.REFERENCE, DisplayType.N2NREFERENCE)) {
            if (MetadataHelper.isCommonsField(refFrom)) continue;

            Entity refEntity = refFrom.getReferenceEntity();
            String entityLabel = String.format("%s (%s)",
                    EasyMetaFactory.getLabel(refEntity), EasyMetaFactory.getLabel(refFrom));
            temp.add(new String[] {
                    refEntity.getName(), entityLabel, refFrom.getName(), FieldWriteback.ONE2ONE_MODE });
            unique.add(refEntity.getName() + "." + refFrom.getName());
        }

        MetadataSorter.sortEntities(temp, null);
        List<Object[]> entities = new ArrayList<>(temp);
        temp.clear();

        // 2.谁引用了我 (N)

        for (Field refTo : MetadataHelper.getReferenceToFields(sourceEntity, true)) {
            String key = refTo.getOwnEntity().getName() + "." + refTo.getName();
            if (unique.contains(key)) {
                log.warn("None unique-key in {}, ignored", sourceEntity);
            } else {
                String entityLabel = String.format("%s (%s) (N)",
                        EasyMetaFactory.getLabel(refTo.getOwnEntity()), EasyMetaFactory.getLabel(refTo));
                temp.add(new String[] {
                        refTo.getOwnEntity().getName(), entityLabel, refTo.getName() });
            }
        }

        MetadataSorter.sortEntities(temp, null);
        entities.addAll(temp);
        temp.clear();

        // 3. 自己
        MetadataSorter.sortEntities(temp, sourceEntity);
        entities.addAll(temp);
        temp.clear();

        // v35 字段匹配
        if (getBoolParameter(request, "matchfields")) {
            for (Entity entity : MetadataSorter.sortEntities(null, false, true)) {
                temp.add(new String[]{entity.getName(), EasyMetaFactory.getLabel(entity), "$"});
            }

            MetadataSorter.sortEntities(temp, null);
            entities.addAll(temp);
            temp.clear();
        }

        return RespBody.ok(entities);
    }

    @RequestMapping("field-writeback-fields")
    public JSON getTargetFields(@EntityParam(name = "source") Entity sourceEntity, HttpServletRequest request) {
        String target = getParameter(request, "target");
        Entity targetEntity = StringUtils.isBlank(target) ? null : MetadataHelper.getEntity(target);

        // 源实体字段
        JSONArray sourceFields = MetaFormatter.buildFieldsWithRefs(sourceEntity, 3, true, field -> {
            if (field instanceof EasyField) {
                EasyField easyField = (EasyField) field;
                return easyField.getDisplayType() == DisplayType.BARCODE
                        || MetaFormatter.isSystemField4Hide(easyField.getRawMeta());
            }
            return false;
        });
        // ID
        sourceFields.fluentAdd(0, EasyMetaFactory.toJSON(sourceEntity.getPrimaryField()));

        // 目标实体字段
        JSONArray targetFields = new JSONArray();
        JSONArray targetFields4Group = new JSONArray();
        if (targetEntity != null) {
            targetFields = MetaFormatter.buildFieldsWithRefs(targetEntity, 1, true, field -> {
                EasyField easyField = (EasyField) field;
                return easyField.getDisplayType() == DisplayType.BARCODE || easyField.isBuiltin();
            });

            targetFields4Group = MetaFormatter.buildFieldsWithRefs(targetEntity, 2, false, field -> {
                if (field instanceof EasyField) {
                    EasyField easyField = (EasyField) field;
                    return easyField.getDisplayType() == DisplayType.BARCODE
                            || MetaFormatter.isSystemField4Hide(easyField.getRawMeta());
                }
                return false;
            });
        }

        // 审批流程启用
        boolean hadApproval = targetEntity != null && RobotApprovalManager.instance.hadApproval(
                ObjectUtils.defaultIfNull(targetEntity.getMainEntity(), targetEntity), null) != null;

        return JSONUtils.toJSONObject(
                new String[]{"source", "target", "hadApproval", "target4Group"},
                new Object[]{sourceFields, targetFields, hadApproval, targetFields4Group});
    }

    // 验证公式

    @PostMapping("verify-formula")
    public RespBody verifyFormula(HttpServletRequest request) {
        String formula = ServletUtils.getRequestString(request);
        formula = formula.replace("\\n", "\n");
        String sourceEntity = getParameter(request, "entity");

        JSONObject item = JSONUtils.toJSONObject(
                new String[] { "calcMode", "sourceFormula" },
                new String[] { "FORMULA", formula });

        try {
            Object evalValue = new AggregationEvaluator(
                    item, MetadataHelper.getEntity(sourceEntity), "(1=1)")
                    .evalFormula();
            return RespBody.ok(evalValue);

        } catch (Exception ex) {
            if (ex instanceof AssertFailedException) return RespBody.ok();

            String errMsg = ex.getLocalizedMessage();
            if (errMsg == null) errMsg = "UNABLE TO EVALUATE";

            log.warn("Verify formula error : {} >> {} >> {}", sourceEntity, formula, errMsg);
            return RespBody.error(errMsg);
        }
    }
}
