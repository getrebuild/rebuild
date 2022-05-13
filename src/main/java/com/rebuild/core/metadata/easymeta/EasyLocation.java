/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.utils.JSONUtils;

/**
 * ADDRESS[$$$$LNG,LAT]
 *
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyLocation extends EasyField implements MixValue {
    private static final long serialVersionUID = -3380324396602087075L;

    protected EasyLocation(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return value.toString().split(MetadataHelper.SPLITER_RE)[0];
        }

        return super.convertCompatibleValue(value, targetField);
    }

    @Override
    public Object wrapValue(Object value) {
        if (value == null) return null;
        String[] vals = value.toString().split(MetadataHelper.SPLITER_RE);

        JSONObject mixVal = JSONUtils.toJSONObject("text", vals[0]);
        if (vals.length >= 2) {
            String[] lnglat = vals[vals.length - 1].split(",");
            mixVal.put("lng", lnglat[0]);
            mixVal.put("lat", lnglat.length == 2 ? lnglat[1] : null);
        }
        return mixVal;
    }
}
