/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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
import cn.devezhao.persist4j.dialect.FieldType;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;

/**
 * 字段的前台显示属性
 *
 * @author devezhao
 * @since 2019/7/17
 */
public class FieldPortalAttrs {

    public static final FieldPortalAttrs instance = new FieldPortalAttrs();
    private FieldPortalAttrs() {}

    /**
     * 表单布局
     *
     * @param field
     * @return
     */
    public boolean allowForm(EasyMeta field) {
        if (disallowAll(field)) {
            return false;
        }
        return true;
    }

    /**
     * 列表
     *
     * @param field
     * @return
     */
    public boolean allowDataList(EasyMeta field) {
        if (disallowAll(field) || isPasswd(field)) {
            return false;
        }
        return true;
    }

    /**
     * 搜索
     *
     * @param field
     * @return
     */
    public boolean allowSearch(EasyMeta field) {
        if (disallowAll(field) || isPasswd(field)) {
            return false;
        }
        return true;
    }

    /**
     * @param field
     * @return
     */
    private boolean disallowAll(EasyMeta field) {
        String fieldName = field.getName();
        return ((Field) field.getBaseMeta()).getType() == FieldType.ANY_REFERENCE
                || EntityHelper.ApprovalState.equalsIgnoreCase(fieldName);
    }

    /**
     * @param field
     * @return
     */
    private boolean isPasswd(EasyMeta field) {
        String fieldName = field.getName();
        return fieldName.contains("password") || fieldName.contains("passwd");
    }
}
