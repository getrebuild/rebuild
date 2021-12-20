/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.CascadeModel;
import cn.devezhao.persist4j.metadata.impl.FieldImpl;
import cn.devezhao.persist4j.util.StringHelper;
import cn.devezhao.persist4j.util.support.Table;
import com.alibaba.fastjson.JSON;
import com.hankcs.hanlp.HanLP;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.SetUser;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.setup.Installer;
import com.rebuild.utils.BlockList;
import com.rebuild.utils.RbAssert;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.CharSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 创建字段
 *
 * @author zhaofang123@gmail.com
 * @since 08/13/2018
 */
@Slf4j
public class Field2Schema extends SetUser {

    // 小数位真实长度
    private static final int DECIMAL_SCALE = 8;

    final protected Set<ID> recordedMetaId = new HashSet<>();

    public Field2Schema() {
        super();
    }

    public Field2Schema(ID user) {
        super.setUser(user);
    }

    @Override
    public ID getUser() {
        ID user = super.getUser();
        RbAssert.isAllow(UserHelper.isSuperAdmin(user), Language.L("仅超级管理员可操作"));
        return user;
    }

    /**
     * @param entity
     * @param fieldLabel
     * @param type
     * @param comments
     * @param refEntity
     * @param extConfig
     * @return
     */
    public String createField(Entity entity, String fieldLabel, DisplayType type, String comments, String refEntity, JSON extConfig) {
        long count;
        if ((count = checkRecordCount(entity)) > 100000) {
            throw new MetadataModificationException(Language.L("实体记录过多 (%d)，增加/删除字段可能导致表损坏", count));
        }

        String fieldName = toPinyinName(fieldLabel);
        for (int i = 0; i < 5; i++) {
            if (entity.containsField(fieldName) || MetadataHelper.isCommonsField(fieldName)) {
                fieldName += RandomUtils.nextInt(99);
            } else {
                break;
            }
        }

        Field field = createUnsafeField(
                entity, fieldName, fieldLabel, type, true, true, true, true, true, comments, refEntity, null, extConfig, null);

        boolean schemaReady = schema2Database(entity, new Field[]{field});
        if (!schemaReady) {
            Application.getCommonsService().delete(recordedMetaId.toArray(new ID[0]));
            throw new MetadataModificationException(Language.L("无法同步元数据到数据库"));
        }

        MetadataHelper.getMetadataFactory().refresh(false);
        return fieldName;
    }

    /**
     * @param field
     * @param force
     * @return
     */
    public boolean dropField(Field field, boolean force) {
        EasyField easyMeta = EasyMetaFactory.valueOf(field);
        ID metaRecordId = easyMeta.getMetaId();
        if (easyMeta.isBuiltin() || metaRecordId == null) {
            throw new MetadataModificationException(Language.L("系统内置，不允许删除"));
        }

        Entity entity = field.getOwnEntity();
        if (entity.getNameField().equals(field)) {
            throw new MetadataModificationException(Language.L("名称字段不允许删除"));
        }

        if (!force) {
            long count;
            if ((count = checkRecordCount(entity)) > 100000) {
                throw new MetadataModificationException(Language.L("实体记录过多 (%d)，增加/删除字段可能导致表损坏", count));
            }
        }

        String ddl = String.format("alter table `%s` drop column `%s`", entity.getPhysicalName(), field.getPhysicalName());
        try {
            Application.getSqlExecutor().execute(ddl, 10 * 60);
        } catch (Throwable ex) {
            if (ThrowableUtils.getRootCause(ex).getLocalizedMessage().contains("exists")) {
                log.warn("Column not exists? " + ex.getLocalizedMessage());
            } else {
                log.error("DDL ERROR : \n" + ddl, ex);
                return false;
            }
        }

        Application.getBean(MetaFieldService.class).delete(metaRecordId);
        MetadataHelper.getMetadataFactory().refresh(false);
        return true;
    }

    /**
     * @param entity
     * @return
     */
    protected long checkRecordCount(Entity entity) {
        String sql = String.format("select count(%s) from %s", entity.getPrimaryField().getName(), entity.getName());
        Object[] count = Application.createQueryNoFilter(sql).unique();
        return ObjectUtils.toLong(count[0]);
    }

    /**
     * @param entity
     * @param fields
     * @return
     */
    public boolean schema2Database(Entity entity, Field[] fields) {
        Dialect dialect = Application.getPersistManagerFactory().getDialect();
        final Table table = new Table(entity, dialect);
        final String alterSql = "alter table `" + entity.getPhysicalName() + "`";

        // H2 只能一个个字段的加
        if (Installer.isUseH2() && fields.length > 1) {
            for (Field field : fields) {
                StringBuilder ddl = new StringBuilder(alterSql).append(" add column ");
                table.generateFieldDDL(field, ddl);

                try {
                    Application.getSqlExecutor().executeBatch(new String[]{ddl.toString()}, 10 * 60);
                } catch (Throwable ex) {
                    log.error("DDL ERROR : \n" + ddl, ex);
                    return false;
                }
            }

            return true;
        }

        StringBuilder ddl = new StringBuilder(alterSql);
        for (Field field : fields) {
            ddl.append("\n  add column ");
            table.generateFieldDDL(field, ddl);
            ddl.append(",");
        }
        ddl.deleteCharAt(ddl.length() - 1);

        try {
            Application.getSqlExecutor().executeBatch(new String[]{ddl.toString()}, 10 * 60);
        } catch (Throwable ex) {
            log.error("DDL ERROR : \n" + ddl, ex);
            return false;
        }
        return true;
    }

    /**
     * 内部用。注意此方法不会添加列到数据库
     *
     * @param entity
     * @param fieldName
     * @param fieldLabel
     * @param dt
     * @param nullable
     * @param creatable
     * @param updatable
     * @param repeatable
     * @param queryable
     * @param comments
     * @param refEntity
     * @param cascade
     * @param extConfig
     * @param defaultValue
     * @return
     * @see #createField(Entity, String, DisplayType, String, String, JSON)
     */
    public Field createUnsafeField(Entity entity, String fieldName, String fieldLabel, DisplayType dt,
                                   boolean nullable, boolean creatable, boolean updatable, boolean repeatable, boolean queryable, String comments, String refEntity, CascadeModel cascade,
                                   JSON extConfig, Object defaultValue) {
        if (dt == DisplayType.SERIES || EntityHelper.AutoId.equalsIgnoreCase(fieldName)) {
            nullable = false;
            creatable = false;
            updatable = false;
            repeatable = false;
        } else if (dt == DisplayType.BARCODE) {
            nullable = true;
            creatable = false;
            updatable = false;
            queryable = true;
        }else if (EntityHelper.QuickCode.equalsIgnoreCase(fieldName)) {
            creatable = false;
            queryable = false;
        }

        String physicalName = StringHelper.hyphenate(fieldName).toUpperCase();

        Record recordOfField = EntityHelper.forNew(EntityHelper.MetaField, getUser());
        recordOfField.setString("belongEntity", entity.getName());
        recordOfField.setString("fieldName", fieldName);
        recordOfField.setString("physicalName", physicalName);
        recordOfField.setString("fieldLabel", fieldLabel);
        recordOfField.setString("displayType", dt.name());
        recordOfField.setBoolean("nullable", nullable);
        recordOfField.setBoolean("creatable", creatable);
        recordOfField.setBoolean("updatable", updatable);
        recordOfField.setBoolean("repeatable", repeatable);
        recordOfField.setBoolean("queryable", queryable);

        if (StringUtils.isNotBlank(comments)) {
            recordOfField.setString("comments", comments);
        }
        if (defaultValue != null) {
            recordOfField.setString("defaultValue", defaultValue.toString());
        }

        if (dt == DisplayType.PICKLIST) {
            refEntity = "PickList";
        } else if (dt == DisplayType.CLASSIFICATION) {
            refEntity = "ClassificationData";
        }

        if (extConfig != null) {
            recordOfField.setString("extConfig", extConfig.toJSONString());
        }

        if (StringUtils.isNotBlank(refEntity)) {
            // 忽略验证实体是否存在
            // 在导入实体时需要，需自行保证引用实体有效性，否则系统会出错
            if (!DynamicMetadataContextHolder.isSkipRefentityCheck(false)) {
                if (!MetadataHelper.containsEntity(refEntity)) {
                    throw new MetadataModificationException(Language.L("无效引用实体 : %s", refEntity));
                }
            }

            recordOfField.setString("refEntity", refEntity);
            if (cascade != null) {
                String cascadeAlias = cascade == CascadeModel.RemoveLinks ? "remove-links" : cascade.name().toLowerCase();
                recordOfField.setString("cascade", cascadeAlias);
            } else {
                recordOfField.setString("cascade", CascadeModel.Ignore.name().toLowerCase());
            }
        }

        int maxLength = dt.getMaxLength();
        if (EntityHelper.QuickCode.equalsIgnoreCase(fieldName)) {
            maxLength = 70;
        }
        recordOfField.setInt("maxLength", maxLength);

        if ((dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE) && StringUtils.isBlank(refEntity)) {
            throw new MetadataModificationException(Language.L("引用字段必须指定引用实体"));
        }

        recordOfField = Application.getCommonsService().create(recordOfField);
        recordedMetaId.add(recordOfField.getPrimary());

        // 以下会改变一些属性，因为并不想他们保存在元数据中

        boolean autoValue = EntityHelper.AutoId.equalsIgnoreCase(fieldName);

        // 系统级字段非空
        nullable = !MetadataHelper.isCommonsField(fieldName)
                || (MetadataHelper.isApprovalField(fieldName) || fieldName.equalsIgnoreCase(EntityHelper.QuickCode));

        // 审批状态默认值
        if (!EntityHelper.ApprovalState.equalsIgnoreCase(fieldName)) {
            defaultValue = null;
        }

        Field unsafeField = new FieldImpl(fieldName, physicalName, fieldLabel, null,
                creatable, updatable, Boolean.TRUE, entity, dt.getFieldType(), maxLength, CascadeModel.Ignore,
                nullable, repeatable, autoValue, DECIMAL_SCALE, defaultValue);
        if (entity instanceof UnsafeEntity) {
            ((UnsafeEntity) entity).addField(unsafeField);
        }
        return unsafeField;
    }

    /**
     * 中文 -> 拼音（仅保留字母数字）
     * 全英文+数字直接返回，不支持的字符会使用随机数
     *
     * @param text
     * @return
     */
    protected String toPinyinName(final String text) {
        String identifier = text;
        if (text.length() < 4) {
            identifier = "rb" + text + RandomUtils.nextInt(10);
        }

        // 全英文直接返回
        if (identifier.matches("[a-zA-Z0-9]+")) {
            if (!CharSet.ASCII_ALPHA.contains(identifier.charAt(0)) || BlockList.isBlock(identifier)) {
                identifier = "rb" + identifier;
            }
            return identifier;
        }

        identifier = HanLP.convertToPinyinString(identifier, "", false);
        identifier = identifier.replaceAll("[^a-zA-Z0-9]", "");
        if (StringUtils.isBlank(identifier)) {
            identifier = String.valueOf(System.currentTimeMillis() / 1000);
        }

        char start = identifier.charAt(0);
        if (!CharSet.ASCII_ALPHA.contains(start)) {
            identifier = "rb" + identifier;
        }

        identifier = identifier.toLowerCase();
        if (identifier.length() > 42) {
            identifier = identifier.substring(0, 42);
        } else if (identifier.length() < 4) {
            identifier += RandomUtils.nextInt(9999);
        }

        return identifier;
    }
}
