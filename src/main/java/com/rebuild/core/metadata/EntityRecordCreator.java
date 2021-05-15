/*
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
import com.rebuild.core.service.DataSpecificationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.rebuild.core.support.i18n.Language.$L;

/**
 * 标准 Record 解析
 *
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-26
 */
@Slf4j
public class EntityRecordCreator extends JsonRecordCreator {

    // 严格模式
    private boolean strictMode;

    /**
     * @param entity
     * @param source
     * @param editor
     */
    public EntityRecordCreator(Entity entity, JSONObject source, ID editor) {
        this(entity, source, editor, false);
    }

    /**
     * @param entity
     * @param source
     * @param editor
     * @param strictMode
     */
    public EntityRecordCreator(Entity entity, JSONObject source, ID editor, boolean strictMode) {
        super(entity, source, editor);
        this.strictMode = strictMode;
    }

    @Override
    public boolean onSetFieldValueWarn(Field field, String value, Record record) {
        boolean isNew = record.getPrimary() == null;

        // 明细关联主记录 ID
        if (isNew && field.getType() == FieldType.REFERENCE && record.getEntity().getMainEntity() != null) {
            Field dtf = MetadataHelper.getDetailToMainField(record.getEntity());
            return field.equals(dtf);
        }

        if (strictMode) {
            return false;
        } else {
            return !MetadataHelper.isCommonsField(field);
        }
    }

    @Override
    protected void afterCreate(Record record) {
        EntityHelper.bindCommonsFieldsValue(record, record.getPrimary() == null);
        verify(record);
    }

    @Override
    public void verify(Record record) {
        List<String> notAllowed = new ArrayList<>();
        // 新建
        if (record.getPrimary() == null) {
            // 自动只读字段可以忽略非空检查
            final Set<String> roAutos = EasyMetaFactory.getAutoReadonlyFields(record.getEntity().getName());

            for (Field field : entity.getFields()) {
                if (MetadataHelper.isSystemField(field) || roAutos.contains(field.getName())) {
                    continue;
                }

                final EasyField easyField = EasyMetaFactory.valueOf(field);
                if (easyField.getDisplayType() == DisplayType.SERIES) {
                    continue;
                }

                Object hasVal = record.getObjectValue(field.getName());
                if ((hasVal == null || NullValue.is(hasVal)) && !field.isNullable()) {
                    notAllowed.add(easyField.getLabel());
                }
            }

            if (!notAllowed.isEmpty()) {
                throw new DataSpecificationException(
                        $L("%s 不允许为空", StringUtils.join(notAllowed, " / ")));
            }
        }
        // 更新
        else {
            for (String fieldName : record.getAvailableFields()) {
                Field field = record.getEntity().getField(fieldName);
                if (EntityHelper.ModifiedOn.equalsIgnoreCase(fieldName)
                        || EntityHelper.ModifiedBy.equalsIgnoreCase(fieldName)
                        || field.getType() == FieldType.PRIMARY) {
                    continue;
                }

                final EasyField easyField = EasyMetaFactory.valueOf(field);
                if (!easyField.isUpdatable()) {
                    if (strictMode) {
                        notAllowed.add(easyField.getLabel());
                    } else {
                        record.removeValue(fieldName);
                        log.warn("Remove non-updatable field : " + fieldName);
                    }
                }
            }

            if (!notAllowed.isEmpty()) {
                throw new DataSpecificationException(
                        $L("%s 不允许修改", StringUtils.join(notAllowed, " / ")));
            }
        }
    }
}