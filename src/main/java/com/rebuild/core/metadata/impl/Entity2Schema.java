/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.CascadeModel;
import cn.devezhao.persist4j.util.support.Table;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.License;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;

import static com.rebuild.core.support.i18n.Language.$L;

/**
 * 创建实体
 *
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
@Slf4j
public class Entity2Schema extends Field2Schema {

    /**
     * @param user
     */
    public Entity2Schema(ID user) {
        super(user);
    }

    /**
     * @param entityLabel
     * @param comments
     * @param mainEntity
     * @param haveNameField
     * @return
     */
    public String createEntity(String entityLabel, String comments, String mainEntity, boolean haveNameField) {
        return createEntity(null, entityLabel, comments, mainEntity, haveNameField);
    }

    /**
     * @param entityName
     * @param entityLabel
     * @param comments
     * @param mainEntity
     * @param haveNameField
     * @return returns 实体名称
     */
    public String createEntity(String entityName, String entityLabel, String comments, String mainEntity, boolean haveNameField) {
        if (entityName != null) {
            if (MetadataHelper.containsEntity(entityName)) {
                throw new MetadataModificationException($L("实体已存在 : %s", entityName));
            }

        } else {
            entityName = toPinyinName(entityLabel);
            for (int i = 0; i < 5; i++) {
                if (MetadataHelper.containsEntity(entityName)) {
                    entityName += RandomUtils.nextInt(99);
                } else {
                    break;
                }
            }
        }

        final boolean isDetail = StringUtils.isNotBlank(mainEntity);
        if (isDetail && !MetadataHelper.containsEntity(mainEntity)) {
            throw new MetadataModificationException($L("无效主实体 : %s", mainEntity));
        }

        String physicalName = "T__" + entityName.toUpperCase();

        Object[] maxTypeCode = Application.createQueryNoFilter(
                "select min(typeCode) from MetaEntity").unique();
        int typeCode = maxTypeCode == null || ObjectUtils.toInt(maxTypeCode[0]) == 0
                ? 999 : (ObjectUtils.toInt(maxTypeCode[0]) - 1);
        if (typeCode <= (License.isCommercial() ? 500 : 900)) {
            throw new MetadataModificationException("ENTITY CODE EXCEEDS SYSTEM LIMIT : " + typeCode);
        }

        // 名称字段
        String nameFiled = EntityHelper.CreatedOn;
        if (haveNameField) {
            nameFiled = entityName + "Name";
        }

        Record record = EntityHelper.forNew(EntityHelper.MetaEntity, user);
        record.setString("entityLabel", entityLabel);
        record.setString("entityName", entityName);
        record.setString("physicalName", physicalName);
        record.setInt("typeCode", typeCode);
        if (StringUtils.isNotBlank(comments)) {
            record.setString("comments", comments);
        }
        if (isDetail) {
            record.setString("masterEntity", mainEntity);
        }
        record.setString("nameField", nameFiled);
        record = Application.getCommonsService().create(record);
        tempMetaId.add(record.getPrimary());

        Entity tempEntity = new UnsafeEntity(entityName, physicalName, entityLabel, typeCode, nameFiled);
        try {
            String primaryFiled = entityName + "Id";
            createBuiltinField(tempEntity, primaryFiled, "ID", DisplayType.ID, null, null, null);
            // 自增ID
            createBuiltinField(tempEntity, EntityHelper.AutoId, "AUTOID", DisplayType.NUMBER, null, null, null);

            if (haveNameField) {
                createUnsafeField(
                        tempEntity, nameFiled, $L("%s名称", entityLabel), DisplayType.TEXT, false, true, true, true, true, null, null, null, null, null);
            }

            createBuiltinField(tempEntity, EntityHelper.CreatedBy, $L("创建人"), DisplayType.REFERENCE, null, "User", null);
            createBuiltinField(tempEntity, EntityHelper.CreatedOn, $L("创建时间"), DisplayType.DATETIME, null, null, null);
            createBuiltinField(tempEntity, EntityHelper.ModifiedBy, $L("修改人"), DisplayType.REFERENCE, null, "User", null);
            createBuiltinField(tempEntity, EntityHelper.ModifiedOn, $L("修改时间"), DisplayType.DATETIME, null, null, null);

            // 明细实体关联字段
            // 明细实体无所属用户或部门，使用主实体的
            if (isDetail) {
                String mainLabel = EasyMetaFactory.valueOf(mainEntity).getLabel();
                String mainPrimary = mainEntity + "Id";
                createBuiltinField(tempEntity, mainPrimary, mainLabel, DisplayType.REFERENCE, $L("引用主记录"), mainEntity, CascadeModel.Delete);
            } else {
                // 助记码/搜索码
                createUnsafeField(
                        tempEntity, EntityHelper.QuickCode, $L("助记码"), DisplayType.TEXT, true, false, false, true, false, null, null, null, null, null);

                createBuiltinField(tempEntity, EntityHelper.OwningUser, $L("所属用户"), DisplayType.REFERENCE, null, "User", null);
                createBuiltinField(tempEntity, EntityHelper.OwningDept, $L("所属部门"), DisplayType.REFERENCE, null, "Department", null);
            }
        } catch (Throwable ex) {
            log.error(null, ex);
            Application.getCommonsService().delete(tempMetaId.toArray(new ID[0]));
            throw new MetadataModificationException($L("无法同步元数据到数据库 : %s", ex.getLocalizedMessage()));
        }

        boolean schemaReady = schema2Database(tempEntity);
        if (!schemaReady) {
            Application.getCommonsService().delete(tempMetaId.toArray(new ID[0]));
            throw new MetadataModificationException($L("无法同步元数据到数据库"));
        }

        MetadataHelper.getMetadataFactory().refresh(false);
        return entityName;
    }

    /**
     * @param entity
     * @return
     */
    public boolean dropEntity(Entity entity) {
        return dropEntity(entity, false);
    }

    /**
     * @param entity
     * @param force
     * @return
     */
    public boolean dropEntity(Entity entity, boolean force) {
        if (!user.equals(UserService.ADMIN_USER)) {
            throw new MetadataModificationException($L("仅超级管理员可删除实体"));
        }

        EasyEntity easy = EasyMetaFactory.valueOf(entity);
        ID metaRecordId = easy.getMetaId();
        if (easy.isBuiltin() || metaRecordId == null) {
            throw new MetadataModificationException($L("系统内置，不允许删除"));
        }

        // 强制删除先删除明细实体

        if (entity.getDetailEntity() != null) {
            if (force) {
                log.warn("Force drop detail-entity first : " + entity.getDetailEntity().getName());
                boolean dropDetail = this.dropEntity(entity.getDetailEntity(), true);
                if (dropDetail) {
                    entity = MetadataHelper.getEntity(entity.getEntityCode());
                } else {
                    throw new MetadataModificationException($L("不能直接删除主实体，请先删除明细实体"));
                }

            } else {
                throw new MetadataModificationException($L("不能直接删除主实体，请先删除明细实体"));
            }
        }

        for (Field whoRef : entity.getReferenceToFields(false)) {
            if (whoRef.getOwnEntity().equals(entity)) continue;
            if (whoRef.getType() == FieldType.ANY_REFERENCE) continue;
            throw new MetadataModificationException(
                    $L("实体已被其他实体引用 (引用实体 : %s)", $L(whoRef.getOwnEntity())));
        }

        // 有记录的强删
        if (!force) {
            long count;
            if ((count = checkRecordCount(entity)) > 0) {
                throw new MetadataModificationException($L("不能删除有数据的实体 (记录数 : %d)", count));
            }
        }

        // 先删配置

        final ID sessionUser = UserContextHolder.getUser(true);
        if (sessionUser == null) UserContextHolder.setUser(user);

        try {
            Application.getBean(MetaEntityService.class).delete(metaRecordId);
        } finally {
            if (sessionUser == null) UserContextHolder.clear();
        }

        // 最后删表

        String ddl = String.format("drop table if exists `%s`", entity.getPhysicalName());
        try {
            Application.getSqlExecutor().execute(ddl, 10 * 60);
        } catch (Throwable ex) {
            log.error("DDL ERROR : \n" + ddl, ex);
            return false;
        }

        MetadataHelper.getMetadataFactory().refresh(false);
        return true;
    }

    /**
     * @param entity
     * @param fieldName
     * @param fieldLabel
     * @param displayType
     * @param comments
     * @param refEntity
     * @param cascade
     * @return
     */
    private Field createBuiltinField(Entity entity, String fieldName, String fieldLabel, DisplayType displayType, String comments,
                                     String refEntity, CascadeModel cascade) {
        return createUnsafeField(
                entity, fieldName, fieldLabel, displayType, false, false, false, true, true, comments, refEntity, cascade, null, null);
    }

    /**
     * @param entity
     * @return
     */
    private boolean schema2Database(Entity entity) {
        Dialect dialect = Application.getPersistManagerFactory().getDialect();
        Table table = new Table(entity, dialect);
        String[] ddls = table.generateDDL(false, false, false);
        try {
            Application.getSqlExecutor().executeBatch(ddls);
        } catch (Throwable ex) {
            log.error("DDL Error : \n" + StringUtils.join(ddls, "\n"), ex);
            return false;
        }
        return true;
    }
}
