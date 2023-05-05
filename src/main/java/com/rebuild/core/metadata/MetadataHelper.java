/*!
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
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.metadata.impl.GhostEntity;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 实体元数据
 *
 * @author Zixin (RB)
 * @see EasyMetaFactory
 * @since 08/13/2018
 */
@Slf4j
public class MetadataHelper {

    // 通用分隔符
    public static final String SPLITER = CommonsUtils.COMM_SPLITER;
    public static final String SPLITER_RE = CommonsUtils.COMM_SPLITER_RE;

    // 实体类型 https://getrebuild.com/docs/admin/entity/#%E5%AE%9E%E4%BD%93%E7%B1%BB%E5%9E%8B
    public static final int TYPE_BAD = -1;
    public static final int TYPE_SYS = 0;
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_MAIN = 2;
    public static final int TYPE_DETAIL = 3;

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
            throw new MissingMetaExcetion(Language.L("实体 [%s] 已经不存在，请检查配置", entityName.toUpperCase()));
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
            throw new MissingMetaExcetion(Language.L("实体 [%s] 已经不存在，请检查配置", entityCode));
        }
    }

    /**
     * @param recordId
     * @return
     */
    public static String getEntityName(ID recordId) {
        return getEntity(recordId.getEntityCode()).getName();
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
                    Language.L("字段 [%s] 已经不存在，请检查配置", (entityName + "#" + fieldName).toUpperCase()));
        }
    }

    /**
     * <tt>reference</tt> 的哪些字段引用了 <tt>source</tt>
     *
     * @param sourceEntity
     * @param referenceEntity
     * @param includeN2N 包括多引用
     * @return
     */
    public static Field[] getReferenceToFields(Entity sourceEntity, Entity referenceEntity, boolean includeN2N) {
        List<Field> fields = new ArrayList<>();
        for (Field field : referenceEntity.getFields()) {
            boolean isRef = field.getType() == FieldType.REFERENCE
                    || (includeN2N && field.getType() == FieldType.REFERENCE_LIST);
            if (isRef && field.getReferenceEntity().equals(sourceEntity)) {
                fields.add(field);
            }
        }
        return fields.toArray(new Field[0]);
    }

    /**
     * @param sourceEntity
     * @param referenceEntity
     * @return
     * @see #getReferenceToFields(Entity, Entity, boolean)
     */
    public static Field[] getReferenceToFields(Entity sourceEntity, Entity referenceEntity) {
        return getReferenceToFields(sourceEntity, referenceEntity, Boolean.FALSE);
    }

    /**
     * 哪些字段引用了 <tt>source</tt>
     *
     * @param sourceEntity
     * @param includeN2N
     * @return
     * @see #getReferenceToFields(Entity, Entity, boolean)
     */
    public static Field[] getReferenceToFields(Entity sourceEntity, boolean includeN2N) {
        List<Field> fields = new ArrayList<>();
        for (Entity entity : getEntities()) {
            CollectionUtils.addAll(fields, getReferenceToFields(sourceEntity, entity, includeN2N));
        }
        return fields.toArray(new Field[0]);
    }

    /**
     * 仅供系统使用的字段，用户不可见/不可用
     *
     * @param field
     * @return
     * @see #getDetailToMainField(Entity) DMT 字段也属于系统字段，但此方法未做处理
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
        return isSystemField(field) || isCommonsField(field.getName());
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
                || EntityHelper.ApprovalStepNode.equalsIgnoreCase(fieldName)
                || EntityHelper.ApprovalLastUser.equalsIgnoreCase(fieldName)
                || EntityHelper.ApprovalLastTime.equalsIgnoreCase(fieldName)
                || EntityHelper.ApprovalLastRemark.equalsIgnoreCase(fieldName);
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
     * @param detailEntity
     * @return
     */
    public static Field getDetailToMainField(Entity detailEntity) {
        Entity main = detailEntity.getMainEntity();
        Assert.isTrue(main != null, "None detail-entity");

        String mainForeign = main.getName() + "Id";
        if (detailEntity.containsField(mainForeign)) return detailEntity.getField(mainForeign);

        for (Field field : detailEntity.getFields()) {
            if (field.getType() != FieldType.REFERENCE) continue;

            // 不可建的那个才是，因为明细字段也可能引用主实体
            if (main.equals(field.getReferenceEntity()) && !field.isCreatable()) {
                return field;
            }
        }
        throw new MetadataException("Bad detail-entity (No DTM)");
    }

    /**
     * @param entity
     * @param fieldPath
     * @return
     * @see #getLastJoinField(Entity, String, boolean)
     */
    public static Field getLastJoinField(Entity entity, String fieldPath) {
        return getLastJoinField(entity, fieldPath, Boolean.FALSE);
    }

    /**
     * 点连接字段（如 owningUser.loginName），获取最后一个字段。
     * 此方法也可以用来判断点连接字段是否是有效的字段（无效返回空）
     *
     * @param entity
     * @param fieldPath
     * @param compatibleN2N 兼容多引用
     * @return
     */
    public static Field getLastJoinField(Entity entity, String fieldPath, boolean compatibleN2N) {
        final String[] ps = fieldPath.split("\\.");

        if (fieldPath.charAt(0) == QueryCompiler.NAME_FIELD_PREFIX) {
            ps[0] = ps[0].substring(1);
            if (!entity.containsField(ps[0])) return null;
        }

        Field lastField = null;
        Entity father = entity;
        for (String field : ps) {
            if (father != null && father.containsField(field)) {
                lastField = father.getField(field);
                if (lastField.getType() == FieldType.REFERENCE) {
                    father = lastField.getReferenceEntity();
                } else if (compatibleN2N && lastField.getType() == FieldType.REFERENCE_LIST) {
                    father = lastField.getReferenceEntity();
                }  else {
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
        if (entity.containsField(fieldName)) return true;
        log.warn("Unknown field `{}` in `{}`", fieldName, entity.getName());
        CommonsUtils.printStackTrace();
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
        if (!containsEntity(entityName)) return false;
        return checkAndWarnField(getEntity(entityName), fieldName);
    }

    /**
     * 实体分类标签
     * TODO 性能
     *
     * @return
     */
    public static Set<String> getEntityTags() {
        Set<String> set = new TreeSet<>();
        for (Entity entity : getEntities()) {
            String tags = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.TAGS);
            if (StringUtils.isNotBlank(tags)) {
                Collections.addAll(set, tags.split(","));
            }
        }
        return set;
    }

    /**
     * @param entityCode
     * @return
     */
    public static int getEntityType(int entityCode) {
        if (containsEntity(entityCode)) return getEntityType(getEntity(entityCode));
        return TYPE_BAD;
    }

    /**
     * @param entity
     * @return
     */
    public static int getEntityType(Entity entity) {
        if (entity.getMainEntity() != null) return TYPE_DETAIL;
        if (entity.getDetailEntity() != null) return TYPE_MAIN;
        if (hasPrivilegesField(entity)) return TYPE_NORMAL;
        return TYPE_SYS;
    }

    /**
     * @param idtext
     * @param entityCode
     * @return
     */
    public static ID checkSpecEntityId(String idtext, int entityCode) {
        if (!ID.isId(idtext)) return null;
        ID id = ID.valueOf(idtext);
        return id.getEntityCode() == entityCode ? id : null;
    }
}
