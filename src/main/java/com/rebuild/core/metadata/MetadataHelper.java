/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MetadataException;
import cn.devezhao.persist4j.query.compiler.QueryCompiler;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.DynamicMetadataFactory;
import com.rebuild.core.metadata.impl.GhostEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体元数据
 *
 * @author zhaofang123@gmail.com
 * @see EasyMetaFactory
 * @since 08/13/2018
 */
public class MetadataHelper {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataHelper.class);

    /**
     * 元数据工厂
     *
     * @return
     */
    public static DynamicMetadataFactory getMetadataFactory() {
        return (DynamicMetadataFactory) Application.getPersistManagerFactory().getMetadataFactory();
    }

    /**
     * 全部实体
     *
     * @return
     */
    public static Entity[] getEntities() {
        return getMetadataFactory().getEntities();
    }

    /**
     * @param entityName
     * @return
     */
    public static boolean containsEntity(String entityName) {
        try {
            return !(getEntity(entityName) instanceof GhostEntity);
        } catch (MetadataException ex) {
            return false;
        }
    }

    /**
     * @param entityCode
     * @return
     */
    public static boolean containsEntity(int entityCode) {
        try {
            getEntity(entityCode);
            return true;
        } catch (MetadataException ex) {
            return false;
        }
    }

    /**
     * @param entityName
     * @param fieldName
     * @return
     */
    public static boolean containsField(String entityName, String fieldName) {
        try {
            return getEntity(entityName).containsField(fieldName);
        } catch (MetadataException ex) {
            return false;
        }
    }

    /**
     * @param entityName
     * @return
     * @throws MetadataException If not exists
     */
    public static Entity getEntity(String entityName) throws MetadataException {
        return getMetadataFactory().getEntity(entityName);
    }

    /**
     * @param entityCode
     * @return
     * @throws MetadataException If not exists
     */
    public static Entity getEntity(int entityCode) throws MetadataException {
        return getMetadataFactory().getEntity(entityCode);
    }

    /**
     * @param record
     * @return
     */
    public static String getEntityName(ID record) {
        return getEntity(record.getEntityCode()).getName();
    }

    /**
     * @param entityName
     * @param fieldName
     * @return
     */
    public static Field getField(String entityName, String fieldName) {
        return getEntity(entityName).getField(fieldName);
    }

    /**
     * {@link Entity#getNameField()} 有可能返回空，应优先使用此方法
     *
     * @param entity
     * @return
     */
    public static Field getNameField(Entity entity) {
        Field hasName = entity.getNameField();
        if (hasName != null) {
            return hasName;
        }
        if (entity.containsField(EntityHelper.CreatedOn)) {
            return entity.getField(EntityHelper.CreatedOn);
        }
        return entity.getPrimaryField();
    }

    /**
     * {@link Entity#getNameField()} 有可能返回空，应优先使用此方法
     *
     * @param entity
     * @return
     */
    public static Field getNameField(String entity) {
        return getNameField(getEntity(entity));
    }

    /**
     * <tt>reference</tt> 中的哪些字段引用了 <tt>source</tt>
     *
     * @param source
     * @param reference
     * @return
     */
    public static Field[] getReferenceToFields(Entity source, Entity reference) {
        List<Field> fields = new ArrayList<>();
        for (Field field : reference.getFields()) {
            if (field.getType() != FieldType.REFERENCE) {
                continue;
            }

            if (field.getReferenceEntity().getEntityCode().equals(source.getEntityCode())) {
                fields.add(field);
            }
        }
        return fields.toArray(new Field[0]);
    }

    /**
     * 仅供系统使用的字段，用户不可见/不可用
     *
     * @param field
     * @return
     */
    public static boolean isSystemField(Field field) {
        return isSystemField(field.getName()) || field.getType() == FieldType.PRIMARY;
    }

    /**
     * 仅供系统使用的字段，用户不可见/不可用
     *
     * @param fieldName
     * @return
     */
    public static boolean isSystemField(String fieldName) {
        return EntityHelper.AutoId.equalsIgnoreCase(fieldName)
                || EntityHelper.QuickCode.equalsIgnoreCase(fieldName)
                || EntityHelper.IsDeleted.equalsIgnoreCase(fieldName)
                || EntityHelper.ApprovalStepNode.equalsIgnoreCase(fieldName);
    }

    /**
     * 是否公共字段
     *
     * @param field
     * @return
     * @see #isSystemField(Field)
     * @see EntityHelper
     */
    public static boolean isCommonsField(Field field) {
        if (isSystemField(field)) return true;
        return isCommonsField(field.getName());
    }

    /**
     * 是否公共字段
     *
     * @param fieldName
     * @return
     * @see #isSystemField(Field)
     * @see EntityHelper
     */
    public static boolean isCommonsField(String fieldName) {
        if (isSystemField(fieldName) || isApprovalField(fieldName)) {
            return true;
        }
        return EntityHelper.OwningUser.equalsIgnoreCase(fieldName) || EntityHelper.OwningDept.equalsIgnoreCase(fieldName)
                || EntityHelper.CreatedOn.equalsIgnoreCase(fieldName) || EntityHelper.CreatedBy.equalsIgnoreCase(fieldName)
                || EntityHelper.ModifiedOn.equalsIgnoreCase(fieldName) || EntityHelper.ModifiedBy.equalsIgnoreCase(fieldName);
    }

    /**
     * 是否审批流程字段
     *
     * @param fieldName
     * @return
     * @see #hasApprovalField(Entity)
     */
    public static boolean isApprovalField(String fieldName) {
        return EntityHelper.ApprovalId.equalsIgnoreCase(fieldName)
                || EntityHelper.ApprovalState.equalsIgnoreCase(fieldName)
                || EntityHelper.ApprovalStepNode.equalsIgnoreCase(fieldName);
    }

    /**
     * 是否 Bizz 实体。即 User Department Role Team
     *
     * @param entityCode
     * @return
     */
    public static boolean isBizzEntity(int entityCode) {
        return entityCode == EntityHelper.User || entityCode == EntityHelper.Department
                || entityCode == EntityHelper.Role || entityCode == EntityHelper.Team;
    }

    /**
     * 是否 Bizz 实体
     *
     * @param entityName
     * @return
     */
    public static boolean isBizzEntity(String entityName) {
        return "User".equalsIgnoreCase(entityName) || "Department".equalsIgnoreCase(entityName)
                || "Role".equalsIgnoreCase(entityName) || "Team".equalsIgnoreCase(entityName);
    }

    /**
     * 是否业务实体
     *
     * @param entity
     * @return
     * @see #hasPrivilegesField(Entity)
     * @see com.rebuild.core.metadata.easymeta.EasyEntity#isPlainEntity()
     */
    public static boolean isBusinessEntity(Entity entity) {
        return hasPrivilegesField(entity) || EasyMetaFactory.valueOf(entity).isPlainEntity();
    }

    /**
     * 实体是否具备权限字段（业务实体）
     *
     * @param entity
     * @return
     */
    public static boolean hasPrivilegesField(Entity entity) {
        return entity.containsField(EntityHelper.OwningUser) && entity.containsField(EntityHelper.OwningDept);
    }

    /**
     * 实体启用了具备审批流程
     *
     * @param entity
     * @return
     */
    public static boolean hasApprovalField(Entity entity) {
        return entity.containsField(EntityHelper.ApprovalId) && entity.containsField(EntityHelper.ApprovalState);
    }

    /**
     * 获取明细实体哪个字段引用自主实体
     *
     * @param detail
     * @return
     */
    public static Field getDetailToMainField(Entity detail) {
        Entity main = detail.getMainEntity();
        Assert.isTrue(main != null, "None detail-entity");

        for (Field field : detail.getFields()) {
            if (field.getType() != FieldType.REFERENCE) {
                continue;
            }
            // 不可建的那个才是，因为明细字段也可能引用主实体
            if (main.equals(field.getReferenceEntity()) && !field.isCreatable()) {
                return field;
            }
        }
        throw new MetadataException("Bad detail entity (No DTM)");
    }

    /**
     * 点连接字段（如 owningUser.loginName），获取最后一个字段。
     * 此方法也可以用来判断点连接字段是否是有效的字段
     *
     * @param entity
     * @param fieldPath
     * @return
     */
    public static Field getLastJoinField(Entity entity, String fieldPath) {
        String[] paths = fieldPath.split("\\.");
        if (fieldPath.charAt(0) == QueryCompiler.NAME_FIELD_PREFIX) {
            paths[0] = paths[0].substring(1);
            if (!entity.containsField(paths[0])) {
                return null;
            }
        }

        Field lastField = null;
        Entity father = entity;
        for (String field : paths) {
            if (father != null && father.containsField(field)) {
                lastField = father.getField(field);
                if (lastField.getType() == FieldType.REFERENCE) {
                    father = lastField.getReferenceEntity();
                } else {
                    father = null;
                }
            } else {
                return null;
            }
        }
        return lastField;
    }

    /**
     * 检查字段有效性（无效会 LOG）
     *
     * @param entity
     * @param fieldName
     * @return
     */
    public static boolean checkAndWarnField(Entity entity, String fieldName) {
        if (entity.containsField(fieldName)) {
            return true;
        }
        LOG.warn("Unknown field '" + fieldName + "' in '" + entity + "'");
        return false;
    }

    /**
     * 检查字段有效性（无效会 LOG）
     *
     * @param entityName
     * @param fieldName
     * @return
     */
    public static boolean checkAndWarnField(String entityName, String fieldName) {
        if (!containsEntity(entityName)) {
            return false;
        }
        return checkAndWarnField(getEntity(entityName), fieldName);
    }
}
