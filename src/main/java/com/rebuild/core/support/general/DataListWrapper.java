/*!
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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据包装
 *
 * @author Zhao Fangfang
 * @since 1.0, 2019-6-20
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

    private boolean mixWrapper = true;

    private Map<ID, Object> cacheRefValue = new HashMap<>();

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
            if (!this.useDesensitized) {
                this.useDesensitized = UserHelper.isAdmin(user) && RebuildConfiguration.getBool(ConfigurationItem.SecurityEnhanced);
            }
        }
    }

    /**
     * @return
     */
    public JSON toJson() {
        final Field nameFiled = entity.getNameField();
        final EasyField nameFieldEasy = EasyMetaFactory.valueOf(nameFiled);

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
                if (!checkHasFieldPrivileges(selectFields[colIndex].getField())) {
                    row[colIndex] = FieldValueHelper.NO_READ_PRIVILEGES;
                    continue;
                }
                if (!checkHasJoinFieldPrivileges(selectFields[colIndex], raw)) {
                    row[colIndex] = FieldValueHelper.NO_READ_PRIVILEGES;
                    continue;
                }

                final Object value = row[colIndex];
                if (value == null) {
                    row[colIndex] = StringUtils.EMPTY;
                    continue;
                }

                final SelectItem fieldItem = selectFields[colIndex];
                final Field fieldMeta = fieldItem.getField();

                // 名称字段值
                if (fieldMeta.equals(nameFiled) && !fieldItem.getFieldPath().contains(".")) {
                    nameValue = value;
                }

                // At last
                if (colIndex + 1 == selectFieldsLen && fieldMeta.getType() == FieldType.PRIMARY) {
                    // 字段权限
                    if (checkHasFieldPrivileges(entity.getNameField())) {
                        // 如无名称字段值则补充
                        if (nameValue == null) {
                            nameValue = FieldValueHelper.getLabel((ID) value, StringUtils.EMPTY);
                        } else {
                            nameValue = FieldValueHelper.wrapFieldValue(nameValue, nameFiled, true);
                        }
                        if (nameValue != null && isUseDesensitized(nameFieldEasy)) {
                            nameValue = FieldValueHelper.desensitized(nameFieldEasy, nameValue);
                        }
                    } else {
                        nameValue = FieldValueHelper.NO_READ_PRIVILEGES;
                    }

                    ((ID) value).setLabel(ObjectUtils.defaultIfNull(nameValue, StringUtils.EMPTY));
                }

                row[colIndex] = wrapFieldValue(value, fieldMeta);
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
     * @see FieldValueHelper#wrapFieldValue(Object, EasyField, boolean)
     */
    protected Object wrapFieldValue(Object value, Field field) {
        EasyField easyField = EasyMetaFactory.valueOf(field);
        if (easyField.getDisplayType() == DisplayType.ID) {
            return FieldValueHelper.wrapMixValue((ID) value, null);
        }

        final DisplayType dt = easyField.getDisplayType();
        final Object originValue = value;
        final boolean isCacheRefValue = dt == DisplayType.REFERENCE && value instanceof ID;

        if (isCacheRefValue) {
            if (cacheRefValue.containsKey((ID) value)) return cacheRefValue.get(value);
        }

        boolean unpack = dt == DisplayType.CLASSIFICATION || dt == DisplayType.PICKLIST
                || dt == DisplayType.STATE || dt == DisplayType.BOOL;

        value = FieldValueHelper.wrapFieldValue(value, easyField, unpack);

        if (value != null) {
            if (isUseDesensitized(easyField)) {
                value = FieldValueHelper.desensitized(easyField, value);
            }

            // v3.1.3 引用字段使用名称字段作为脱敏依据
            if (this.useDesensitized
                    && (dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE)) {
                Field useNameField = easyField.getRawMeta().getReferenceEntity().getNameField();
                EasyField useNameFieldEasy = EasyMetaFactory.valueOf(useNameField);
                if (useNameFieldEasy.isDesensitized()) {
                    FieldValueHelper.desensitizedMixValue(useNameFieldEasy, (JSON) value);
                }
            }
        }

        // v2.10 Color
        if (value != null && this.mixWrapper) {
            if (easyField.getDisplayType() == DisplayType.PICKLIST) {
                String color = PickListManager.instance.getColor((ID) originValue);
                if (StringUtils.isNotBlank(color)) {
                    value = JSONUtils.toJSONObject(
                            new String[]{ "text", "color" }, new Object[]{ value, color });
                }

            } else if (easyField.getDisplayType() == DisplayType.MULTISELECT) {
                // @see MultiSelectManager#getLabels

                List<Object> colorLabels = new ArrayList<>();
                ConfigBean[] entries = MultiSelectManager.instance.getPickListRaw(field, false);
                for (ConfigBean e : entries) {
                    long m = e.get("mask", Long.class);
                    if (((long) originValue & m) != 0) {
                        String text = e.getString("text");
                        String color = e.getString("color");

                        if (StringUtils.isBlank(color)) {
                            colorLabels.add(text);
                        } else {
                            colorLabels.add(JSONUtils.toJSONObject(
                                    new String[]{ "text", "color" }, new Object[]{ text, color }));
                        }
                    }
                }

                ((JSONObject) value).put("text", colorLabels);

            } else if (easyField.getDisplayType() == DisplayType.TAG) {

                Map<String, String> colorNames = TagSupport.getNamesColor(easyField);
                List<Object> colorValue = new ArrayList<>();
                for (Object o : (JSONArray) value) {
                    String name = o.toString();
                    colorValue.add(JSONUtils.toJSONObject(
                            new String[]{ "name", "color" }, new Object[]{ name, colorNames.get(name) }));
                }
                value = colorValue;

            } else  if (easyField.getDisplayType() == DisplayType.CLASSIFICATION) {
                ClassificationManager.Item item = ClassificationManager.instance.getItem((ID) originValue);
                if (item != null && StringUtils.isNotBlank(item.getColor())) {
                    value = JSONUtils.toJSONObject(
                            new String[]{ "text", "color" },
                            new Object[]{ value, item.getColor() });
                }
            }
        }

        if (isCacheRefValue) cacheRefValue.put((ID) originValue, value);
        return value;
    }

    /**
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
     * @see #checkHasFieldPrivileges(Field)
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

    /**
     * @param field
     * @return
     */
    protected boolean checkHasFieldPrivileges(Field field) {
        ID u = user == null ? UserContextHolder.getUser() : user;
        return Application.getPrivilegesManager().getFieldPrivileges().isReadable(field, u);
    }

    /**
     * 进一步封装查询结果
     *
     * @param mixWrapper
     */
    public void setMixWrapper(boolean mixWrapper) {
        this.mixWrapper = mixWrapper;
    }
}
