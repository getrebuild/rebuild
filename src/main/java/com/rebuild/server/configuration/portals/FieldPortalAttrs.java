/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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
     * 表单
     */
    public static final String TYPE_FORM = "FORM";
    /**
     * 列表
     */
    public static final String TYPE_DATALIST = "DATALIST";
    /**
     * 搜索
     */
    public static final String TYPE_SEARCH = "SEARCH";
    
    /**
     * @param field
     * @param type
     * @return
     */
    public boolean allowByType(Field field, String type) {
    	if (TYPE_FORM.equalsIgnoreCase(type)) {
    		return allowForm(field);
    	} else if (TYPE_DATALIST.equalsIgnoreCase(type)) {
    		return allowDataList(field);
    	} else if (TYPE_SEARCH.equalsIgnoreCase(type)) {
    		return allowSearch(field);
    	}
    	return true;
    }
    
    /**
     * @param field
     * @return
     */
    public boolean allowForm(Field field) {
        if (disallowAll(field)) {
            return false;
        }
        return true;
    }

    /**
     * @param field
     * @return
     */
    public boolean allowDataList(Field field) {
        if (disallowAll(field) || isPasswd(field)) {
            return false;
        }
        return true;
    }

    /**
     * @param field
     * @return
     */
    public boolean allowSearch(Field field) {
        if (disallowAll(field) || isPasswd(field)) {
            return false;
        }
        return true;
    }

    /**
     * @param field
     * @return
     */
    private boolean disallowAll(Field field) {
        String fieldName = field.getName();
        return field.getType() == FieldType.ANY_REFERENCE
                || EntityHelper.ApprovalStepNode.equalsIgnoreCase(fieldName);
    }

    /**
     * @param field
     * @return
     */
    private boolean isPasswd(Field field) {
        String fieldName = field.getName();
        return fieldName.contains("password") || fieldName.contains("passwd");
    }
}
