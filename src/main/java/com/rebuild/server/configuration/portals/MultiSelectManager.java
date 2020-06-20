/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.configuration.portals;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 多选字段
 *
 * @author ZHAO
 * @since 2019/9/27
 */
public class MultiSelectManager extends PickListManager {

    public static final MultiSelectManager instance = new MultiSelectManager();
    protected MultiSelectManager() { }

    /**
     * @param field
     * @return
     */
    public JSONArray getSelectList(Field field) {
        ConfigEntry[] entries = getPickListRaw(field, false);
        for (ConfigEntry e : entries) {
            e.set("hide", null).set("id", null);
        }
        return JSONUtils.toJSONArray(entries);
    }

    /**
     * @param maskValue
     * @param field
     * @return
     */
    public String[] getLabel(long maskValue, Field field) {
        if (maskValue <= 0) {
            return new String[0];
        }

        List<String> labels = new ArrayList<>();
        ConfigEntry[] entries = getPickListRaw(field, false);
        for (ConfigEntry e : entries) {
            long m = e.get("mask", Long.class);
            if ((maskValue & m) != 0) {
                labels.add(e.getString("text"));
            }
        }
        return labels.toArray(new String[0]);
    }

    /**
     * @param label
     * @param field
     * @return
     */
    public Long findByLabel(String label, Field field) {
        Object[] o = Application.createQueryNoFilter(
                "select maskValue from PickList where belongEntity = ? and belongField = ? and text = ?")
                .setParameter(1, field.getOwnEntity().getName())
                .setParameter(2, field.getName())
                .setParameter(3, label)
                .unique();
        return o == null ? null : (Long) o[0];
    }

    /**
     * 获取默认值
     *
     * @param field
     * @return
     */
    public Long getDefaultValue(Field field) {
        long maskValue = 0;
        for (ConfigEntry e : getPickListRaw(field, false)) {
            if (e.getBoolean("default")) {
                maskValue += e.get("mask", Long.class);
            }
        }
        return maskValue == 0 ? null : maskValue;
    }

    @Override
    public void clean(Object cacheKey) {
        if (cacheKey instanceof ID) {
            // Nothings
        } else {
            super.clean(cacheKey);
        }
    }
}
