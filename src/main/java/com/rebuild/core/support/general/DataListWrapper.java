/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.query.compiler.SelectItem;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * 数据包装
 *
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-20
 */
public class DataListWrapper {

    final protected int total;
    final protected Object[][] data;
    final protected SelectItem[] selectFields;
    final protected Entity entity;

    // for 权限验证
    protected ID user;
    protected Map<String, Integer> queryJoinFields;

    // 信息脱敏
    protected boolean useDesensitized = false;

    /**
     * @param total
     * @param data
     * @param selectFields
     * @param entity
     */
    public DataListWrapper(int total, Object[][] data, SelectItem[] selectFields, Entity entity) {
        this.total = total;
        this.data = data;
        this.selectFields = selectFields;
        this.entity = entity;
    }

    /**
     * 设置权限过滤（针对引用字段）
     *
     * @param user
     * @param joinFields
     */
    protected void setPrivilegesFilter(ID user, Map<String, Integer> joinFields) {
        if (user != null && joinFields != null && !joinFields.isEmpty()) {
            this.user = user;
            this.queryJoinFields = joinFields;
        }

        if (user != null) {
            this.useDesensitized = !Application.getPrivilegesManager().allow(user, ZeroEntry.AllowNoDesensitized);
        }
    }

    /**
     * @return
     */
    public JSON toJson() {
        final Field nameFiled = entity.getNameField();
        final int joinFieldsLen = queryJoinFields == null ? 0 : queryJoinFields.size();
        final int selectFieldsLen = selectFields.length - joinFieldsLen;

        for (int rowIndex = 0; rowIndex < data.length; rowIndex++) {
            final Object[] raw = data[rowIndex];

            Object[] row = raw;
            if (joinFieldsLen > 0) {
                row = new Object[selectFieldsLen];
                System.arraycopy(raw, 0, row, 0, selectFieldsLen);
                data[rowIndex] = row;
            }

            Object nameValue = null;
            for (int colIndex = 0; colIndex < selectFieldsLen; colIndex++) {
                if (!checkHasJoinFieldPrivileges(selectFields[colIndex], raw)) {
                    row[colIndex] = FieldValueHelper.NO_READ_PRIVILEGES;
                    continue;
                }

                final Object value = row[colIndex];
                if (value == null) {
                    row[colIndex] = StringUtils.EMPTY;
                    continue;
                }

                SelectItem fieldItem = selectFields[colIndex];
                Field field = fieldItem.getField();
                if (field.equals(nameFiled) && !fieldItem.getFieldPath().contains(".")) {
                    nameValue = value;
                }

                // 如果最终没能取得名称字段，则补充
                if (field.getType() == FieldType.PRIMARY) {
                    if (nameValue == null) {
                        nameValue = FieldValueHelper.getLabel((ID) value, StringUtils.EMPTY);
                    } else {
                        nameValue = FieldValueHelper.wrapFieldValue(nameValue, nameFiled, true);
                        if (nameValue == null) {
                            nameValue = StringUtils.EMPTY;
                        }
                    }
                    ((ID) value).setLabel(nameValue);
                }

                if (field.getType() == FieldType.REFERENCE_LIST) {
                    ID valueUseRecordId = (ID) raw[raw.length - 1];
                    row[colIndex] = wrapFieldValue(valueUseRecordId, field);
                } else {
                    row[colIndex] = wrapFieldValue(value, field);
                }
            }
        }

        return JSONUtils.toJSONObject(
                new String[] { "total", "data" },
                new Object[] { total, data });
    }

    /**
     * @param value
     * @param field
     * @return
     */
    protected Object wrapFieldValue(Object value, Field field) {
        EasyField easyField = EasyMetaFactory.valueOf(field);
        if (easyField.getDisplayType() == DisplayType.ID) {
            return FieldValueHelper.wrapMixValue((ID) value, null);
        }

        boolean unpack = easyField.getDisplayType() == DisplayType.CLASSIFICATION
                || easyField.getDisplayType() == DisplayType.PICKLIST
                || easyField.getDisplayType() == DisplayType.STATE
                || easyField.getDisplayType() == DisplayType.BOOL;

        value = FieldValueHelper.wrapFieldValue(value, easyField, unpack);

        if (value != null && isUseDesensitized(easyField)) {
            value = FieldValueHelper.desensitized(easyField, value);
        }
        return value;
    }

    /**
     * @param easyField
     * @return
     * @see FieldValueHelper#isUseDesensitized(EasyField, ID)
     */
    private boolean isUseDesensitized(EasyField easyField) {
        return this.useDesensitized && easyField.isDesensitized();
    }

    /**
     * 验证（引用）字段权限
     *
     * @param field
     * @param original
     * @return
     */
    protected boolean checkHasJoinFieldPrivileges(SelectItem field, Object[] original) {
        if (this.queryJoinFields == null || UserHelper.isAdmin(user)) {
            return true;
        }

        String[] fieldPath = field.getFieldPath().split("\\.");
        if (fieldPath.length == 1) {
            return true;
        }

        int fieldIndex = queryJoinFields.get(fieldPath[0]);
        Object check = original[fieldIndex];
        return check == null || Application.getPrivilegesManager().allowRead(user, (ID) check);
    }
}
