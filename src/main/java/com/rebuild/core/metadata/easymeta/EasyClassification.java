/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.support.general.FieldValueHelper;
import org.apache.commons.lang.StringUtils;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyClassification extends EasyField implements MixValue {
    private static final long serialVersionUID = -2295351268412805467L;

    protected EasyClassification(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            JSONObject wrapped = (JSONObject) wrapValue(value);
            return wrapped.getString("text");
        }

        return super.convertCompatibleValue(value, targetField);
    }

    @Override
    public Object wrapValue(Object value) {
        ID id = (ID) value;
        String text = StringUtils.defaultIfBlank(
                ClassificationManager.instance.getFullName(id), FieldValueHelper.MISS_REF_PLACE);
        return FieldValueHelper.wrapMixValue(id, text);
    }

    @Override
    public Object exprDefaultValue() {
        String valueExpr = (String) getRawMeta().getDefaultValue();
        return ID.isId(valueExpr) ? ID.valueOf(valueExpr) : null;
    }
}
