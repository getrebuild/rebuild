/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tools;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.aibot.Config;
import com.rebuild.utils.JSONUtils;

/**
 * @author Zixin
 * @since 2025/5/10
 */
public class RecordUpsert implements FunctionCalling {

    private final Entity entity;

    public RecordUpsert(Entity entity) {
        this.entity = entity;
    }

    @Override
    public JSONObject toAiJSON() {
        JSONObject c = Config.getDeepSeekFc("record_upsert", "新建或更新记录，用户需要提供必填的信息");
        JSONObject parameters = c.getJSONObject("function").getJSONObject("parameters");
        JSONObject properties = parameters.getJSONObject("properties");
        JSONArray required = parameters.getJSONArray("required");

        for (Field field : entity.getFields()) {
            if (MetadataHelper.isCommonsField(field)) continue;

            EasyField e = EasyMetaFactory.valueOf(field);
            DisplayType dt = e.getDisplayType();
            if (dt == DisplayType.SERIES || dt == DisplayType.BARCODE
                    || dt == DisplayType.SIGN || dt == DisplayType.AVATAR) continue;

            String type = "string";

            JSONObject m = JSONUtils.toJSONObject(
                    new String[]{"type", "description"},
                    new String[]{type, e.getLabel()});

            properties.put(field.getName(), m);
            if (!e.isNullable()) required.add(field.getName());
        }
        return c;
    }
}
