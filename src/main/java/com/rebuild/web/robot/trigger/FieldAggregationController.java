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
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.general.MetaFormatter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@RestController
@RequestMapping("/admin/robot/trigger/")
public class FieldAggregationController extends BaseController {

    @RequestMapping("field-aggregation-entities")
    public List<String[]> getTargetEntities(@EntityParam(name = "source") Entity sourceEntity) {
        List<String[]> entities = new ArrayList<>();

        // 1. 我引用了谁

        for (Field refFrom : MetadataSorter.sortFields(sourceEntity, DisplayType.REFERENCE)) {
            if (MetadataHelper.isApprovalField(refFrom.getName())) continue;

            Entity refEntity = refFrom.getReferenceEntity();
            String entityLabel = String.format("%s (%s)",
                    EasyMetaFactory.getLabel(refEntity), EasyMetaFactory.getLabel(refFrom));
            entities.add(new String[] { refEntity.getName(), entityLabel, refFrom.getName() });
        }

        // v3.0 字段聚合无需自己
        sortEntities(entities, null);
        return entities;
    }

    @RequestMapping("field-aggregation-fields")
    public JSON getTargetFields(@EntityParam(name = "source") Entity sourceEntity,
                                HttpServletRequest request) {
        String target = getParameter(request, "target");
        Entity targetEntity = StringUtils.isBlank(target) ? null : MetadataHelper.getEntity(target);

        // 源字段

        JSONArray sourceFields = MetaFormatter.buildFieldsWithRefs(sourceEntity, 3, field -> {
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
            for (Field field : MetadataSorter.sortFields(targetEntity,
                    DisplayType.NUMBER, DisplayType.DECIMAL, DisplayType.DATE, DisplayType.DATETIME,
                    DisplayType.N2NREFERENCE, DisplayType.NTEXT)) {
                EasyField easyField = EasyMetaFactory.valueOf(field);
                if (easyField.isBuiltin()) continue;

                JSONObject item = (JSONObject) easyField.toJSON();
                if (easyField.getDisplayType() == DisplayType.ID) {
                    item.put("ref", new String[] {
                            targetEntity.getName(), EasyMetaFactory.getDisplayType(targetEntity.getNameField()).name() });
                }

                targetFields.add(item);
            }
        }

        // 审批流程启用
        boolean hadApproval = targetEntity != null && RobotApprovalManager.instance.hadApproval(
                ObjectUtils.defaultIfNull(targetEntity.getMainEntity(), targetEntity), null) != null;

        return JSONUtils.toJSONObject(
                new String[] { "source", "target", "hadApproval" },
                new Object[] { sourceFields, targetFields, hadApproval });
    }

    /**
     * 排序
     *
     * @param entities
     * @param selfEntity 添加自己
     */
    public static void sortEntities(List<String[]> entities, Entity selfEntity) {
        Comparator<Object> comparator = Collator.getInstance(Locale.CHINESE);
        entities.sort((o1, o2) -> comparator.compare(o1[1], o2[1]));

        // 可更新自己（通过主键字段）
        if (selfEntity != null) {
            entities.add(new String[] {
                    selfEntity.getName(), EasyMetaFactory.getLabel(selfEntity), TriggerAction.SOURCE_SELF });
        }
    }
}
