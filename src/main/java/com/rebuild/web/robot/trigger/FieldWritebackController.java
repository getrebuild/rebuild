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
import com.rebuild.core.service.trigger.impl.AggregationEvaluator;
import com.rebuild.core.service.trigger.impl.FieldWriteback;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.general.MetaFormatter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

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
    public List<String[]> getTargetEntities(@EntityParam(name = "source") Entity sourceEntity) {
        List<String[]> temp = new ArrayList<>();

        // 1. 我引用了谁 v2.7.1

        for (Field refFrom : MetadataSorter.sortFields(sourceEntity, DisplayType.REFERENCE)) {
            if (MetadataHelper.isCommonsField(refFrom)) continue;

            Entity refEntity = refFrom.getReferenceEntity();
            String entityLabel = String.format("%s (%s)",
                    EasyMetaFactory.getLabel(refEntity), EasyMetaFactory.getLabel(refFrom));
            temp.add(new String[] {
                    refEntity.getName(), entityLabel, refFrom.getName(), FieldWriteback.ONE2ONE_MODE});
        }

        FieldAggregationController.sortEntities(temp, null);
        List<String[]> entities = new ArrayList<>(temp);
        temp.clear();

        // 2. 谁引用了我

        for (Field refTo : MetadataHelper.getReferenceToFields(sourceEntity)) {
            String entityLabel = String.format("%s (%s) (N)",
                    EasyMetaFactory.getLabel(refTo.getOwnEntity()), EasyMetaFactory.getLabel(refTo));
            temp.add(new String[] {
                    refTo.getOwnEntity().getName(), entityLabel, refTo.getName() });
        }

        FieldAggregationController.sortEntities(temp, null);
        entities.addAll(temp);
        temp.clear();

        // 3. 自己
        FieldAggregationController.sortEntities(temp, sourceEntity);
        entities.addAll(temp);
        temp.clear();

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

            JSONArray res = MetaFormatter.buildFields(fieldRef);
            if (res != null) sourceFields.addAll(res);
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

                targetFields.add(MetaFormatter.buildRichField(easyField));
            }
        }

        // 审批流程启用
        boolean hadApproval = targetEntity != null
                && RobotApprovalManager.instance.hadApproval(targetEntity, null) != null;

        return JSONUtils.toJSONObject(
                new String[]{"source", "target", "hadApproval"},
                new Object[]{sourceFields, targetFields, hadApproval});
    }

    // 验证公式

    @PostMapping("verify-formula")
    public RespBody verifyFormula(HttpServletRequest request) {
        String formula = ServletUtils.getRequestString(request);
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
            String errMsg = ex.getLocalizedMessage();
            log.warn("Verify formula error : {} >> {} >> {}", sourceEntity, formula, errMsg);
            return RespBody.error(errMsg);
        }
    }
}
