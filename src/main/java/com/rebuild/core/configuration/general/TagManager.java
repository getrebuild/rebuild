/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.metadata.easymeta.EasyTag;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签管理
 *
 * @author ZHAO
 * @since 2022/12/12
 * @see com.rebuild.core.metadata.easymeta.EasyTag
 */
public class TagManager implements ConfigManager {

    public static final TagManager instance = new TagManager();

    private TagManager() {
    }

    /**
     * @param field
     * @return
     */
    public Object getDefaultValue(EasyTag field) {
        JSONArray tagList = field.getExtraAttrs(Boolean.TRUE).getJSONArray(EasyFieldConfigProps.TAG_LIST);
        if (tagList == null || tagList.isEmpty()) return null;

        List<String> dv = new ArrayList<>();
        for (Object o : tagList) {
            JSONObject tag = (JSONObject) o;
            if (tag.getBooleanValue("default")) dv.add(tag.getString("name"));
        }
        return dv.toArray(new String[0]);
    }

    @Override
    public void clean(Object cacheKey) {
        // Notings
    }
}
