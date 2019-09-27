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
        return super.getPickList(field);
    }

    /**
     * @param field
     * @param includeHide
     * @return
     */
    public JSONArray getSelectList(Field field, boolean includeHide) {
        return super.getPickList(field, includeHide);
    }

    /**
     * @param maskValue
     * @param field
     * @return
     */
    public String getLabel(long maskValue, Field field) {
        final String ckey = String.format("MultiSelectLABEL-%s.%s:%d", field.getOwnEntity().getNameField(), field.getName(), maskValue);
        String cval = Application.getCommonCache().get(ckey);
        if (cval != null) {
            return cval;
        }

        Object[] o = Application.createQueryNoFilter(
                "select text from PickList where maskValue = ?")
                .setParameter(1, maskValue)
                .unique();
        if (o != null) {
            cval = (String) o[0];
        }
        Application.getCommonCache().put(ckey, cval);
        return cval;
    }

    /**
     * @param label
     * @param field
     * @return
     */
    public Long findMaskByLabel(String label, Field field) {
        Object[] o = Application.createQueryNoFilter(
                "select maskValue from PickList where belongEntity = ? and belongField = ? and text = ?")
                .setParameter(1, field.getOwnEntity().getName())
                .setParameter(2, field.getName())
                .setParameter(3, label)
                .unique();
        return o == null ? 0L : (Long) o[0];
    }

    /**
     * 获取默认项
     *
     * @param field
     * @return
     */
    public Long getDefaultMask(Field field) {
        for (ConfigEntry e : getPickListRaw(field, false)) {
            if (e.getBoolean("default")) {
                return e.get("mask", Long.class);
            }
        }
        return 0L;
    }

    @Override
    public void clean(Object cacheKey) {
        if (cacheKey instanceof ID) {
            Object[] maskValue = Application.getQueryFactory().uniqueNoFilter((ID) cacheKey, "belongEntity", "belongField", "maskValue");
            if (maskValue != null) {
                final String ckey = String.format("MultiSelectLABEL-%s.%s:%d", maskValue[0], maskValue[1], maskValue[2]);
                Application.getCommonCache().evict(ckey);
            }
        } else {
            super.clean(cacheKey);
        }
    }
}
