/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
        return !disallowAll(field);
    }

    /**
     * @param field
     * @return
     */
    public boolean allowDataList(Field field) {
        return !disallowAll(field) && !isPasswd(field);
    }

    /**
     * @param field
     * @return
     */
    public boolean allowSearch(Field field) {
        return !disallowAll(field) && !isPasswd(field);
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
