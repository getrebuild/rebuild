/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.trigger;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.general.MetaFormatter;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author devezhao
 * @since 2021/6/28
 */
@RestController
@RequestMapping("/admin/robot/trigger/")
public class GroupAggregationController extends BaseController {

    @RequestMapping("group-aggregation-entities")
    public JSON getSourceEntities(@EntityParam(name = "source") Entity sourceEntity) {
        // 任意目标实体
        List<String[]> entities = new ArrayList<>();
        for (Entity entity : MetadataSorter.sortEntities(null, false, true)) {
            if (entity.equals(sourceEntity)) continue;
            entities.add(new String[] { entity.getName(), EasyMetaFactory.getLabel(entity) });
        }

        // 源-分组字段
        JSONArray sourceGroupFields = MetaFormatter.buildFieldsWithRefs(sourceEntity, 3, GF_FILTER);
        paddingType2(sourceGroupFields, sourceEntity);

        // 源-聚合字段
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

        return JSONUtils.toJSONObject(
                new String[] { "targetEntities", "sourceGroupFields", "sourceFields" },
                new Object[] { entities, sourceGroupFields, sourceFields });
    }

    @RequestMapping("group-aggregation-fields")
    public JSON getTargetFields(@EntityParam(name = "target") Entity targetEntity) {
        // 目标-分组字段
        JSONArray targetGroupFields = MetaFormatter.buildFieldsWithRefs(targetEntity, 1, GF_FILTER);
        paddingType2(targetGroupFields, targetEntity);

        // 目标字段
        JSONArray targetFields = MetaFormatter.buildFieldsWithRefs(targetEntity, 1, field -> {
            DisplayType dt = ((EasyField) field).getDisplayType();
            return !(dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL
                    || dt == DisplayType.NTEXT || dt == DisplayType.N2NREFERENCE);
        });

        // 审批流程启用
        boolean hadApproval = RobotApprovalManager.instance.hadApproval(
                ObjectUtils.defaultIfNull(targetEntity.getMainEntity(), targetEntity), null) != null;

        return JSONUtils.toJSONObject(
                new String[] { "targetGroupFields", "targetFields", "hadApproval" },
                new Object[] { targetGroupFields, targetFields, hadApproval });
    }

    // 分组字段类型的支持
    private static final Predicate<BaseMeta> GF_FILTER = field -> {
        if (field instanceof EasyField) {
            DisplayType dt = ((EasyField) field).getDisplayType();
            return !(dt == DisplayType.TEXT
                    || dt == DisplayType.DATE || dt == DisplayType.DATETIME
                    || dt == DisplayType.CLASSIFICATION || dt == DisplayType.REFERENCE);
        }
        return false;
    };

    // type = type+REF
    private static void paddingType2(JSONArray fields, Entity entity) {
        for (Object o : fields) {
            JSONObject item = (JSONObject) o;
            String type = item.getString("type");
            if (DisplayType.REFERENCE.name().equals(type)) {
                String type2 = type + ":" + item.getJSONArray("ref").get(0);
                item.put("type", type2);
            } else if (DisplayType.CLASSIFICATION.name().equals(type)) {
                Field clazz = MetadataHelper.getLastJoinField(entity, item.getString("name"));
                String type2 = type + ":" + EasyMetaFactory.valueOf(clazz).getExtraAttr(EasyFieldConfigProps.CLASSIFICATION_USE);
                item.put("type", type2);
            }
        }
    }
}
