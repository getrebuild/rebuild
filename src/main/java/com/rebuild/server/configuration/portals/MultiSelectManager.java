/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
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
        ConfigEntry entries[] = getPickListRaw(field, false);
        for (ConfigEntry e : entries) {
            e.set("hide", null);
            e.set("id", null);
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
        ConfigEntry entries[] = getPickListRaw(field, false);
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
