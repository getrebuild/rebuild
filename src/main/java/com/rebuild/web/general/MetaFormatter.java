/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.state.StateManager;
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author ZHAO
 * @since 2022/7/22
 */
public class MetaFormatter {

    /**
     * @param field
     * @return
     */
    public static JSONObject buildRichField(EasyField field) {
        JSONObject res = (JSONObject) field.toJSON();

        // 字段选项
        DisplayType dt = field.getDisplayType();

        if (dt == DisplayType.PICKLIST) {
            res.put("options", PickListManager.instance.getPickList(field.getRawMeta()));

        } else if (dt == DisplayType.STATE) {
            res.put("options", StateManager.instance.getStateOptions(field.getRawMeta()));

        } else if (dt == DisplayType.MULTISELECT) {
            res.put("options", MultiSelectManager.instance.getSelectList(field.getRawMeta()));

        } else if (dt == DisplayType.BOOL) {
            JSONArray options = new JSONArray();
            options.add(JSONUtils.toJSONObject(
                    new String[] { "id", "text" },
                    new Object[] { true, Language.L("是") }));
            options.add(JSONUtils.toJSONObject(
                    new String[] { "id", "text" },
                    new Object[] { false, Language.L("否") }));
            res.put("options", options);

        } else if (dt == DisplayType.NUMBER) {
            res.put(EasyFieldConfigProps.NUMBER_NOTNEGATIVE,
                    field.getExtraAttr(EasyFieldConfigProps.NUMBER_FORMAT));
        } else if (dt == DisplayType.DECIMAL) {
            res.put(EasyFieldConfigProps.DECIMAL_FORMAT,
                    field.getExtraAttr(EasyFieldConfigProps.DECIMAL_FORMAT));
        }

        return res;
    }

    /**
     * @param refField
     * @return
     */
    public static JSONArray buildFields(Field refField) {
        Entity refEntity = refField.getReferenceEntity();
        if (refEntity.getEntityCode() == EntityHelper.RobotApprovalConfig) return null;

        String refFieldName = refField.getName() + ".";
        String refFieldLabel = EasyMetaFactory.getLabel(refField) + ".";

        JSONArray res = new JSONArray();
        for (Field field : MetadataSorter.sortFields(refEntity)) {
            EasyField easyField = EasyMetaFactory.valueOf(field);
            if (easyField.getDisplayType() == DisplayType.BARCODE) continue;

            JSONObject subField = (JSONObject) easyField.toJSON();
            subField.put("name", refFieldName + subField.getString("name"));
            subField.put("label", refFieldLabel + subField.getString("label"));
            res.add(subField);
        }
        return res;
    }

    /**
     * @param entity
     * @param deep
     * @param filter
     * @return
     */
    public static JSONArray buildFieldsWithRefs(Entity entity, int deep, Predicate<BaseMeta> filter) {
        return buildFieldsWithRefs(entity, deep, false, filter);
    }

    /**
     * @param entity
     * @param deep
     * @param rich
     * @param filter
     * @return
     */
    public static JSONArray buildFieldsWithRefs(Entity entity, int deep, boolean rich, Predicate<BaseMeta> filter) {
        JSONArray res = new JSONArray();

        // 一级
        for (Field field : MetadataSorter.sortFields(entity)) {
            EasyField easyField = EasyMetaFactory.valueOf(field);
            if (filter.test(easyField)) continue;

            res.add(buildField(easyField, null, rich));
        }
        if (deep < 2) return res;

        List<Object[]> deep3 = new ArrayList<>();

        // 二级
        for (Field field2 : MetadataSorter.sortFields(entity, DisplayType.REFERENCE)) {
            EasyField easyField2 = EasyMetaFactory.valueOf(field2);
            if (filter.test(field2)) continue;

            String[] parents = new String[] {
                    easyField2.getName(), easyField2.getLabel()
            };

            Entity entity2 = field2.getReferenceEntity();
            for (Field field : MetadataSorter.sortFields(entity2)) {
                EasyField easyField = EasyMetaFactory.valueOf(field);
                if (filter.test(easyField)) continue;

                res.add(buildField(easyField, parents, rich));

                if (deep >= 3 && easyField.getDisplayType() == DisplayType.REFERENCE) {
                    deep3.add(new Object[] { parents[0], parents[1], easyField });
                }
            }
        }
        if (deep < 3) return res;

        // 最多三级
        for (Object[] d : deep3) {
            EasyField easyField3 = (EasyField) d[2];
            if (filter.test(easyField3.getRawMeta())) continue;

            String[] parents = new String[] {
                    d[0] + "." + easyField3.getName(), d[1] + "." + easyField3.getLabel()
            };

            Entity entity3 = easyField3.getRawMeta().getReferenceEntity();
            for (Field field : MetadataSorter.sortFields(entity3)) {
                EasyField easyField = EasyMetaFactory.valueOf(field);
                if (filter.test(easyField)) continue;

                JSONObject item = buildField(easyField, parents, rich);
                String name = item.getString("name");
                // 特殊过滤
                if (name.contains("modifiedBy.modifiedBy")) continue;
                if (name.contains("createdBy.createdBy")) continue;
                if (name.contains("createdBy.modifiedBy")) continue;
                if (name.contains("modifiedBy.createdBy")) continue;

                res.add(item);
            }
        }




        return res;
    }

    private static JSONObject buildField(EasyField field, String[] parentsField, boolean rich) {
        JSONObject item = rich ? buildRichField(field) : (JSONObject) field.toJSON();
        if (parentsField != null) {
            item.put("name", parentsField[0] + "." + item.get("name"));
            item.put("label", parentsField[1] + "." + item.get("label"));
        }
        return item;
    }
}
