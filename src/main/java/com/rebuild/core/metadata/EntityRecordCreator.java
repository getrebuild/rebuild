/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import cn.devezhao.persist4j.record.JsonRecordCreator;
import cn.devezhao.persist4j.record.RecordVisitor;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.PatternValue;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 从 JSON 解析 Record
 *
 * @author Zhao Fangfang
 * @since 1.0, 2019-6-26
 * @see com.rebuild.core.support.general.RecordBuilder
 */
@Slf4j
public class EntityRecordCreator extends JsonRecordCreator {

    // 安全URL（即附件/图片不允许外链）
    private final boolean safetyUrl;

    /**
     * @param entity
     * @param source
     * @param editor
     */
    public EntityRecordCreator(Entity entity, JSONObject source, ID editor) {
        this(entity, source, editor, true);
    }

    /**
     * @param entity
     * @param source
     * @param editor
     * @param safedUrl
     */
    public EntityRecordCreator(Entity entity, JSONObject source, ID editor, boolean safedUrl) {
        super(entity, source, editor);
        this.safetyUrl = safedUrl;
    }

    @Override
    public boolean setFieldValue(Field field, String value, Record record) {
        value = setValueByLiteralBefore(field, value, record, true);
        if (value == null) return false;

        return super.setFieldValue(field, value, record);
    }

    @Override
    public boolean onSetFieldValueWarn(Field field, String value, Record record) {
        // 非业务实体
        if (!MetadataHelper.isBusinessEntity(field.getOwnEntity())) return true;

        final boolean isNew = record.getPrimary() == null;

        // 明细关联主记录 + 位置定位
        if (isNew && isForceCreateable(field)) return true;

        // 公共字段前台可能会布局出来
        // 此处忽略检查没问题，因为最后还会复写，即 EntityHelper#bindCommonsFieldsValue
        boolean isCommonField = MetadataHelper.isCommonsField(field);
        if (!isCommonField) return false;

        if (isNew) return true;

        String n = field.getName();
        return !(EntityHelper.OwningUser.equalsIgnoreCase(n) || EntityHelper.OwningDept.equalsIgnoreCase(n)
                || EntityHelper.CreatedBy.equalsIgnoreCase(n) || EntityHelper.CreatedOn.equalsIgnoreCase(n));
    }

    @Override
    protected void afterCreate(Record record) {
        final int e = entity.getEntityCode();

        // 记录验证
        if (MetadataHelper.isBusinessEntity(entity)) {
            verify(record);
        } else if (e == EntityHelper.Feeds || e == EntityHelper.FeedsComment
                || e == EntityHelper.ProjectTask || e == EntityHelper.ProjectTaskComment
                || e == EntityHelper.User || e == EntityHelper.Department || e == EntityHelper.Role || e == EntityHelper.Team) {
            keepFieldValueSafe(record);
        }

        EntityHelper.bindCommonsFieldsValue(record, record.getPrimary() == null);
    }

    @Override
    public void verify(Record record) {
        // 自动只读字段忽略非空检查
        final Set<String> autoReadonlyFields = EasyMetaFactory.getAutoReadonlyFields(entity.getName());

        List<String> notNulls = new ArrayList<>();  // 非空
        List<String> notWells = new ArrayList<>();  // 格式

        // 新建
        if (record.getPrimary() == null) {
            for (Field field : entity.getFields()) {
                if (MetadataHelper.isCommonsField(field)) continue;

                EasyField easyField = EasyMetaFactory.valueOf(field);
                if (easyField.getDisplayType() == DisplayType.SERIES
                        || easyField.getDisplayType() == DisplayType.BARCODE) {
                    continue;
                }

                Object hasVal = record.getObjectValue(field.getName());
                boolean canNull = field.isNullable() || autoReadonlyFields.contains(field.getName());

                if (NullValue.isNull(hasVal)) {
                    if (!canNull) {
                        // fix3.5.1: 有默认值
                        if (easyField.exprDefaultValue() == null) {
                            notNulls.add(easyField.getLabel());
                        }
                    }
                } else {
                    if (field.isCreatable()) {
                        if (easyField instanceof PatternValue) {
                            if (((PatternValue) easyField).checkPattern(hasVal.toString())); // be:4.2.3
                            else notWells.add(easyField.getLabel());
                        }
                    } else {
                        if (!isForceCreateable(field)) {
                            log.warn("Remove non-creatable field : {}", field);
                            record.removeValue(field.getName());
                        }
                    }
                }
            }
        }
        // 更新
        else {
            for (String fieldName : record.getAvailableFields()) {
                Field field = entity.getField(fieldName);
                if (MetadataHelper.isCommonsField(field)) continue;

                Object hasVal = record.getObjectValue(field.getName());
                boolean canNull = field.isNullable() || autoReadonlyFields.contains(field.getName());

                EasyField easyField = EasyMetaFactory.valueOf(field);
                if (NullValue.isNull(hasVal)) {
                    if (!canNull) {
                        notNulls.add(easyField.getLabel());
                    }
                } else {
                    if (field.isUpdatable()) {
                        if (easyField instanceof PatternValue) {
                            if (((PatternValue) easyField).checkPattern(hasVal.toString())); // be:4.2.3
                            else notWells.add(easyField.getLabel());
                        }
                    } else {
                        log.warn("Remove non-updatable field : {}", field);
                        record.removeValue(fieldName);
                    }
                }
            }
        }

        if (!notNulls.isEmpty()) {
            throw new DataSpecificationException(
                    Language.L("%s 不能为空", StringUtils.join(notNulls, " / ")));
        }
        if (!notWells.isEmpty()) {
            throw new DataSpecificationException(
                    Language.L("%s 格式不正确", StringUtils.join(notWells, " / ")));
        }

        keepFieldValueSafe(record);
    }

    // 强制可新建的字段
    private boolean isForceCreateable(Field field) {
        // DTF 字段（明细关联主记录字段）
        if (field.getType() == FieldType.REFERENCE && entity.getMainEntity() != null) {
            return field.equals(MetadataHelper.getDetailToMainField(entity));
        }

        // 启用自动定位的
        EasyField easyField = EasyMetaFactory.valueOf(field);
        if (easyField.getDisplayType() == DisplayType.LOCATION) {
            return BooleanUtils.toBoolean(easyField.getExtraAttr(EasyFieldConfigProps.LOCATION_AUTOLOCATION));
        }

        return false;
    }

    // 是否需要去除外链
    private void keepFieldValueSafe(Record record) {
        for (String fieldName : record.getAvailableFields()) {
            final Object value = record.getObjectValue(fieldName);
            if (NullValue.isNull(value)) continue;

            final EasyField field = EasyMetaFactory.valueOf(entity.getField(fieldName));

            // 不能外链
            // https://github.com/getrebuild/rebuild/issues/596
            if (safetyUrl) {
                if (field.getDisplayType() == DisplayType.IMAGE
                        || field.getDisplayType() == DisplayType.FILE
                        || field.getDisplayType() == DisplayType.AVATAR) {

                    String s = value.toString().toLowerCase();
                    boolean unsafe = CommonsUtils.isExternalUrl(s);
                    if (!unsafe) {
                        s = CodecUtils.urlDecode(s);
                        unsafe = CommonsUtils.isExternalUrl(s);
                    }

                    if (unsafe) {
                        log.warn("Remove not-safe field : {} < {}", field.getRawMeta(), value);
                        record.removeValue(fieldName);
                    }
                }
            }
        }
    }

    @Override
    public boolean isNoValue(String value, Field field) {
        boolean is = super.isNoValue(value, field);
        if (is) return true;

        // 空的文件/图片
        if ("[]".equalsIgnoreCase(value)) {
            DisplayType dt = EasyMetaFactory.getDisplayType(field);
            return dt == DisplayType.IMAGE || dt == DisplayType.FILE;
        }
        return false;
    }

    // --

    /**
     * @param field
     * @param value
     * @param record
     * @param checkPattern
     * @return
     */
    protected static String setValueByLiteralBefore(Field field, String value, Record record, boolean checkPattern) {
        Type fieldType = field.getType();

        // v4.0, 4.2 处理 {CURRENT} 变量
        if (FieldValueHelper.CURRENT.equals(value) || "{@CURRENT}".equals(value)) {
            Object v = FieldValueHelper.getCurrentVarValue(field, record.getEditor());
            if (v instanceof Date) {
                value = CalendarUtils.getUTCDateTimeFormat().format(v);
            } else if (v != null) {
                if (v instanceof ID[]) v = ((ID[]) v)[0];
                value = v.toString();
            }
        }

        // v4.1, v4.2 处理为标准日期格式
        if (StringUtils.isNotBlank(value) && (fieldType == FieldType.DATE || fieldType == FieldType.TIMESTAMP)) {
            Date d = CommonsUtils.parseDate(value);
            if (d == null) {
                log.warn("Cannot parse date from : {}", value);
                value = null;
            } else {
                value = CalendarUtils.getUTCDateTimeFormat().format(d);
            }
        }

        return value;
    }

    /**
     * 更健壮的字段值设置方法
     *
     * @param field
     * @param value
     * @param record
     * @param checkPattern
     * @see #setValueByLiteralBefore(Field, String, Record, boolean)
     * @see RecordVisitor#setValueByLiteral(Field, String, Record)
     */
    public static void setValueByLiteral(Field field, String value, Record record, boolean checkPattern) {
        value = setValueByLiteralBefore(field, value, record, checkPattern);
        if (value == null) return;

        RecordVisitor.setValueByLiteral(field, value, record);
    }
}