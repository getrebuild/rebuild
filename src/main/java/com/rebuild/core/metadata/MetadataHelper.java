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
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import cn.devezhao.persist4j.query.compiler.QueryCompiler;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.DynamicMetadataFactory;
import com.rebuild.core.metadata.impl.GhostEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

import static com.rebuild.core.support.i18n.I18nUtils.$L;

/**
 * 实体元数据
 *
 * @author zhaofang123@gmail.com
 * @see EasyMetaFactory
 * @since 08/13/2018
 */
@Slf4j
public class MetadataHelper {

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
        } catch (MissingMetaExcetion ex) {
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
        } catch (MissingMetaExcetion ex) {
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
        } catch (MissingMetaExcetion ex) {
            return false;
        }
    }

    /**
     * @param entityName
     * @return
     * @throws MissingMetaExcetion If not exists
     */
    public static Entity getEntity(String entityName) throws MissingMetaExcetion {
        try {
            return getMetadataFactory().getEntity(entityName);
        } catch (MissingMetaExcetion ex) {
            throw new MissingMetaExcetion($L("实体 [%s] 已经不存在，请检查配置", entityName));
        }
    }

    /**
     * @param entityCode
     * @return
     * @throws MissingMetaExcetion If not exists
     */
    public static Entity getEntity(int entityCode) throws MissingMetaExcetion {
        try {
            return getMetadataFactory().getEntity(entityCode);
        } catch (MissingMetaExcetion ex) {
            throw new MissingMetaExcetion($L("实体 [%s] 已经不存在，请检查配置", entityCode));
        }
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
     * @throws MissingMetaExcetion If field not exists
     */
    public static Field getField(String entityName, String fieldName) throws MissingMetaExcetion {
        try {
            return getEntity(entityName).getField(fieldName);
        } catch (MissingMetaExcetion ex) {
            throw new MissingMetaExcetion(
                    $L("字段 [%s] 已经不存在，请检查配置", (entityName + "#" + fieldName).toUpperCase()));
        }
    }

    /**
     * <tt>reference</tt> 的哪些字段引用了 <tt>source</tt>
     *
     * @param source
     * @param reference
     * @return
     */
    public static Field[] getReferenceToFields(Entity source, Entity reference) {
        List<Field> fields = new ArrayList<>();
        for (Field field : reference.getFields()) {
            if (field.getType() == FieldType.REFERENCE && field.getReferenceEntity().equals(source)) {
                fields.add(field);
            }
        }
        return fields.toArray(new Field[0]);
    }

    /**
     * 哪些字段引用了 <tt>sourceEntity</tt>
     *
     * @param source
     * @return
     * @see #getReferenceToFields(Entity, Entity)
     */
    public static Field[] getReferenceToFields(Entity source) {
        List<Field> fields = new ArrayList<>();
        for (Entity entity : getEntities()) {
            CollectionUtils.addAll(fields, getReferenceToFields(source, entity));
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
        if (isSystemField(fieldName) || isApprovalField(fieldName)) return true;

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
     * @param entity
     * @return
     */
    public static boolean isBizzEntity(Entity entity) {
        return isBizzEntity(entity.getEntityCode());
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
        if (entity.getMainEntity() != null) entity = entity.getMainEntity();
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
        throw new MetadataException("Bad detail-entity (No DTM)");
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

        log.warn("Unknown field `" + fieldName + "` in `" + entity.getName() + "`");
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
