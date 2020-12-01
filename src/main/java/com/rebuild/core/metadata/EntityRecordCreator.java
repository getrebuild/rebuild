/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import cn.devezhao.persist4j.record.JsonRecordCreator;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 标准 Record 解析
 *
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-26
 */
public class EntityRecordCreator extends JsonRecordCreator {

    private static final Logger LOG = LoggerFactory.getLogger(EntityRecordCreator.class);

    /**
     * 更新时。是移除不允许更新的字段还是抛出异常
     */
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
        // TODO 非系统级字段是否予以通过
        return false;
    }

    @Override
    protected void afterCreate(Record record) {
        bindCommonsFieldsValue(record, record.getPrimary() == null);
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
                        Language.LF("XNotNull", StringUtils.join(notAllowed, " / ")));
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
                        LOG.warn("Remove non-updatable field : " + fieldName);
                    }
                }
            }

            if (!notAllowed.isEmpty()) {
                throw new DataSpecificationException(
                        Language.LF("XNotModify", StringUtils.join(notAllowed, " / ")));
            }
        }
    }

    /**
     * 绑定公用/权限字段值
     *
     * @param r
     * @param isNew
     */
    protected static void bindCommonsFieldsValue(Record r, boolean isNew) {
        final Date now = CalendarUtils.now();
        final Entity entity = r.getEntity();

        if (entity.containsField(EntityHelper.ModifiedOn)) {
            r.setDate(EntityHelper.ModifiedOn, now);
        }
        if (entity.containsField(EntityHelper.ModifiedBy)) {
            r.setID(EntityHelper.ModifiedBy, r.getEditor());
        }

        if (isNew) {
            if (entity.containsField(EntityHelper.CreatedOn)) {
                r.setDate(EntityHelper.CreatedOn, now);
            }
            if (entity.containsField(EntityHelper.CreatedBy)) {
                r.setID(EntityHelper.CreatedBy, r.getEditor());
            }
            if (entity.containsField(EntityHelper.OwningUser)) {
                r.setID(EntityHelper.OwningUser, r.getEditor());
            }
            if (entity.containsField(EntityHelper.OwningDept)) {
                User user = Application.getUserStore().getUser(r.getEditor());
                r.setID(EntityHelper.OwningDept, (ID) user.getOwningDept().getIdentity());
            }
        }
    }
}