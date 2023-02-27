/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import cn.devezhao.persist4j.record.JsonRecordCreator;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.EasyText;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.i18n.Language;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 从 JSON 解析 Record
 *
 * @author Zhao Fangfang
 * @since 1.0, 2018-6-26
 * @see RecordBuilder
 */
@Slf4j
public class EntityRecordCreator extends JsonRecordCreator {

    /**
     * @param entity
     * @param source
     * @param editor
     */
    public EntityRecordCreator(Entity entity, JSONObject source, ID editor) {
        super(entity, source, editor);
    }

    @Override
    public boolean onSetFieldValueWarn(Field field, String value, Record record) {
        // 非业务实体
        if (MetadataHelper.isBusinessEntity(field.getOwnEntity())) return true;

        final boolean isNew = record.getPrimary() == null;

        // 明细关联主记录
        if (isNew && isDtmField(field)) return true;

        // 公共字段前台可能会布局出来
        // 此处忽略检查没问题，因为最后还会复写，即 #bindCommonsFieldsValue
        boolean isCommonField = MetadataHelper.isCommonsField(field);
        if (!isCommonField) return false;

        String fieldName = field.getName();
        return isNew || (!EntityHelper.OwningUser.equalsIgnoreCase(fieldName)
                && !EntityHelper.OwningDept.equalsIgnoreCase(fieldName)
                && !EntityHelper.CreatedBy.equalsIgnoreCase(fieldName)
                && !EntityHelper.CreatedOn.equalsIgnoreCase(fieldName));
    }

    @Override
    protected void afterCreate(Record record) {
        // 业务实体才验证
        if (MetadataHelper.isBusinessEntity(entity)) verify(record);
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
                        notNulls.add(easyField.getLabel());
                    }
                } else {
                    if (field.isCreatable()) {
                        if (!patternMatches(easyField, hasVal)) {
                            notWells.add(easyField.getLabel());
                        }
                    } else {
                        if (!isForceCreateable(field)) {
                            log.warn("Remove non-creatable field : " + field);
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
                        if (!patternMatches(easyField, hasVal)) {
                            notWells.add(easyField.getLabel());
                        }
                    } else {
                        log.warn("Remove non-updatable field : " + field);
                        record.removeValue(fieldName);
                    }
                }
            }
        }

        if (!notNulls.isEmpty()) {
            throw new DataSpecificationException(
                    Language.L("%s 不允许为空", StringUtils.join(notNulls, " / ")));
        }
        if (!notWells.isEmpty()) {
            throw new DataSpecificationException(
                    Language.L("%s 格式不正确", StringUtils.join(notWells, " / ")));
        }

        // TODO 检查引用字段的ID是否正确（是否是其他实体的ID）

    }

    // 明细关联主记录字段
    private boolean isDtmField(Field field) {
        if (field.getType() == FieldType.REFERENCE && entity.getMainEntity() != null) {
            return field.equals(MetadataHelper.getDetailToMainField(entity));
        }
        return false;
    }

    // 强制可新建
    private boolean isForceCreateable(Field field) {
        if (isDtmField(field)) return true;

        // 自定定位
        EasyField easyField = EasyMetaFactory.valueOf(field);
        if (easyField.getDisplayType() == DisplayType.LOCATION) {
            return BooleanUtils.toBoolean(easyField.getExtraAttr(EasyFieldConfigProps.LOCATION_AUTOLOCATION));
        }

        return false;
    }

    // 正则匹配
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean patternMatches(EasyField easyField, Object val) {
        if (!(easyField instanceof EasyText)) return true;

        Pattern patt = ((EasyText) easyField).getPattern();
        return patt == null || patt.matcher((CharSequence) val).matches();
    }
}