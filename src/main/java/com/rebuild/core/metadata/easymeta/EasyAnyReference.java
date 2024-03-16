/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.impl.AnyEntity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.support.general.FieldValueHelper;
import org.springframework.util.Assert;

/**
 * 暂不开放（开放应注意触发器/记录转换等处的填值）
 *
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyAnyReference extends EasyReference {
    private static final long serialVersionUID = -5775035002469908191L;

    protected EasyAnyReference(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return FieldValueHelper.getLabelNotry((ID) value);
        }

        Assert.isTrue(targetField.getDisplayType() == DisplayType.ANYREFERENCE, "type-by-type is must");
        return value;
    }

    @Override
    public JSON toJSON() {
        JSONObject map = (JSONObject) super.toJSON();

        map.put("ref", new String[] { AnyEntity.FLAG, AnyEntity.FLAG });
        return map;
    }
}
