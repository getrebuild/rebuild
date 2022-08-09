/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
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
}
