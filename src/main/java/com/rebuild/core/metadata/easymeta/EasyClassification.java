/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyClassification extends EasyReference {
    private static final long serialVersionUID = -2295351268412805467L;

    protected EasyClassification(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object wrapValue(Object value) {
        JSONObject map = (JSONObject) super.wrapValue(value);
        if (map != null) {
            map.remove("entity");
            ClassificationManager.Item item = ClassificationManager.instance.getItem((ID) value);
            if (item != null && item.getColor() != null) map.put("color", item.getColor());
        }
        return map;
    }

    @Override
    public JSON toJSON() {
        JSONObject map = (JSONObject) super.toJSON();
        map.remove("ref");
        map.put(EasyFieldConfigProps.CLASSIFICATION_USE,
                getExtraAttr(EasyFieldConfigProps.CLASSIFICATION_USE));
        return map;
    }
}
