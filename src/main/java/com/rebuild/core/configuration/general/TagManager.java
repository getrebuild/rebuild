/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.utils.JSONUtils;

/**
 * 标签管理
 *
 * @author ZHAO
 * @since 2022/12/12
 * @see com.rebuild.core.metadata.easymeta.EasyTag
 */
public class TagManager extends PickListManager {

    public static final TagManager instance = new TagManager();

    private TagManager() {
    }

    /**
     * @param field
     * @return
     */
    public JSONArray getTagList(Field field) {
        ConfigBean[] entries = getPickListRaw(field, false);
        for (ConfigBean e : entries) {
            e.set("hide", null).set("id", null);
        }
        return JSONUtils.toJSONArray(entries);
    }
}
