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
    public String getText(long maskValue, Field field) {
        final String ckey = "MultiSelectLABEL-" + field.getName() + "-" + maskValue;
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
     * @param text
     * @param field
     * @return
     */
    public Long findByText(String text, Field field) {
        Object[] o = Application.createQueryNoFilter(
                "select maskValue from PickList where belongEntity = ? and belongField = ? and text = ?")
                .setParameter(1, field.getOwnEntity().getName())
                .setParameter(2, field.getName())
                .setParameter(3, text)
                .unique();
        return o == null ? 0L : (Long) o[0];
    }

    /**
     * 获取默认项
     *
     * @param field
     * @return
     */
    public Long getDefaultValue(Field field) {
        for (ConfigEntry e : getPickListRaw(field.getOwnEntity().getName(), field.getName(), false)) {
            if (e.getBoolean("default")) {
                return e.get("mask", Long.class);
            }
        }
        return 0L;
    }

    @Override
    public void clean(Object cacheKey) {
        // TODO 缓存清理
        super.clean(cacheKey);
    }
}
