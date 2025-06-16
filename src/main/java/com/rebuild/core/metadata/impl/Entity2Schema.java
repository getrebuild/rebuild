/*!
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
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.CascadeModel;
import cn.devezhao.persist4j.metadata.MetadataException;
import cn.devezhao.persist4j.util.support.Table;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.License;
import com.rebuild.core.support.NeedRbvException;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 创建实体
 *
 * @author Zixin (RB)
 * @since 08/03/2018
 */
@Slf4j
public class Entity2Schema extends Field2Schema {

    private final int specEntityCode;

    public Entity2Schema() {
        super();
        this.specEntityCode = 0;
    }

    public Entity2Schema(ID user) {
        this(user, 0);
    }

    public Entity2Schema(ID user, int specEntityCode) {
        super();
        super.setUser(user);
        this.specEntityCode = specEntityCode;
    }

    /**
     * @param entityName [可空]
     * @param entityLabel
     * @param comments
     * @param mainEntity
     * @param haveNameField
     * @param haveSeriesField
     * @return Returns 实体名称
     */
    public String createEntity(String entityName, String entityLabel, String comments, String mainEntity, boolean haveNameField, boolean haveSeriesField) {
        if (!License.isRbvAttached() && MetadataHelper.getEntities().length >= 120) {
            throw new NeedRbvException(Language.L("实体数量超出免费版限制"));
        }

        entityLabel = CommonsUtils.maxstr(StringUtils.trim(entityLabel), 40);
        entityName = StringUtils.trim(entityName);

        if (entityName != null) {
            if (MetadataHelper.containsEntity(entityName)) {
                throw new MetadataModificationException(Language.L("实体已存在 : %s", entityName));
            }
        } else {
            entityName = toPinyinName(entityLabel);
            for (int i = 0; i < 6; i++) {
                if (MetadataHelper.containsEntity(entityName)) {
                    entityName += CommonsUtils.randomInt(0, 9);
                } else {
                    break;
                }
            }
        }

        final boolean isDetail = StringUtils.isNotBlank(mainEntity);
        if (isDetail && !MetadataHelper.containsEntity(mainEntity)) {
            throw new MetadataModificationException(Language.L("无效主实体 : %s", mainEntity));
        }

        final int typeCode = genEntityTypeCode();
        final String physicalName = "T__" + entityName.toUpperCase();

        // 名称字段
        String nameFiled = EntityHelper.CreatedOn;
        if (haveNameField) {
            nameFiled = entityName + "Name";
        }

        Record record = EntityHelper.forNew(EntityHelper.MetaEntity, getUser());
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
        recordedMetaIds.add(record.getPrimary());

        Entity tempEntity = new UnsafeEntity(entityName, physicalName, entityLabel, typeCode, nameFiled);
        try {
            String primaryFiled = entityName + "Id";
            createBuiltinField(tempEntity, primaryFiled, "ID", DisplayType.ID, null, null, null);
            // 自增ID
            createBuiltinField(tempEntity, EntityHelper.AutoId, "AUTOID", DisplayType.NUMBER, null, null, null);

            if (haveNameField) {
                createUnsafeField(
                        tempEntity, nameFiled, Language.L("%s名称", entityLabel), DisplayType.TEXT, false, true, true, true, true, null, null, null, null, null);
            }
            if (haveSeriesField) {
                createUnsafeField(
                        tempEntity, entityName + "No", Language.L("%s编号", entityLabel), DisplayType.SERIES, false, false, false, false, true, null, null, null, null, null);
            }

            createBuiltinField(tempEntity, EntityHelper.CreatedBy, Language.L("创建人"), DisplayType.REFERENCE, null, "User", null);
            createBuiltinField(tempEntity, EntityHelper.CreatedOn, Language.L("创建时间"), DisplayType.DATETIME, null, null, null);
            createBuiltinField(tempEntity, EntityHelper.ModifiedBy, Language.L("修改人"), DisplayType.REFERENCE, null, "User", null);
            createBuiltinField(tempEntity, EntityHelper.ModifiedOn, Language.L("修改时间"), DisplayType.DATETIME, null, null, null);

            // 明细实体关联字段
            // 明细实体无所属用户或部门，使用主实体的
            if (isDetail) {
                String mainLabel = EasyMetaFactory.valueOf(mainEntity).getLabel();
                String mainForeign = mainEntity + "Id";
                createBuiltinField(tempEntity, mainForeign, mainLabel, DisplayType.REFERENCE, Language.L("引用主记录"), mainEntity, CascadeModel.Delete);
            } else {
                // 助记码/搜索码
                createUnsafeField(
                        tempEntity, EntityHelper.QuickCode, Language.L("助记码"), DisplayType.TEXT, true, false, false, true, false, null, null, null, null, null);

                createBuiltinField(tempEntity, EntityHelper.OwningUser, Language.L("所属用户"), DisplayType.REFERENCE, null, "User", null);
                createBuiltinField(tempEntity, EntityHelper.OwningDept, Language.L("所属部门"), DisplayType.REFERENCE, null, "Department", null);
            }

        } catch (Throwable ex) {
            log.error("Error on create fields", ex);
            Application.getCommonsService().delete(recordedMetaIds.toArray(new ID[0]));

            throw new MetadataModificationException(Language.L("无法同步元数据到数据库 : %s", ex.getLocalizedMessage()));
        }

        boolean schemaReady = schema2Database(tempEntity);
        if (!schemaReady) {
            Application.getCommonsService().delete(recordedMetaIds.toArray(new ID[0]));
            throw new MetadataModificationException(Language.L("无法同步元数据到数据库"));
        }

        MetadataHelper.getMetadataFactory().refresh();
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
        getUser();  // Check admin

        EasyEntity easy = EasyMetaFactory.valueOf(entity);
        ID metaRecordId = easy.getMetaId();
        if (easy.isBuiltin() || metaRecordId == null) {
            throw new MetadataModificationException(Language.L("系统内置，不允许删除"));
        }

        // 强制删除先删除明细实体

        if (entity.getDetailEntity() != null) {
            if (force) {
                log.warn("Force drop detail-entity first : {}", entity.getDetailEntity().getName());
                boolean dropDetail = this.dropEntity(entity.getDetailEntity(), true);
                if (dropDetail) {
                    entity = MetadataHelper.getEntity(entity.getEntityCode());
                } else {
                    throw new MetadataModificationException(Language.L("不能直接删除主实体，请先删除明细实体"));
                }

            } else {
                throw new MetadataModificationException(Language.L("不能直接删除主实体，请先删除明细实体"));
            }
        }

        for (Field whoRef : entity.getReferenceToFields(Boolean.FALSE, Boolean.TRUE)) {
            if (whoRef.getOwnEntity().equals(entity)) continue;
            throw new MetadataModificationException(
                    Language.L("实体已被其他实体引用 (引用实体 : %s)", Language.L(whoRef.getOwnEntity())));
        }

        // 有记录的强删
        if (!force) {
            long count;
            if ((count = checkRecordCount(entity)) > 0) {
                throw new MetadataModificationException(Language.L("不能删除有数据的实体 (记录数 : %d)", count));
            }
        }

        // 先删配置

        final ID threadUser = UserContextHolder.getUser(Boolean.TRUE);
        if (threadUser == null) UserContextHolder.setUser(getUser());

        try {
            Application.getBean(MetaEntityService.class).delete(metaRecordId);
        } finally {
            if (threadUser == null) UserContextHolder.clearUser();
        }

        // 最后删表

        String ddl = String.format("drop table if exists `%s`", entity.getPhysicalName());
        try {
            Application.getSqlExecutor().execute(ddl, DDL_TIMEOUT);
        } catch (Throwable ex) {
            log.error("DDL ERROR : \n{}", ddl, ex);
            return false;
        }

        MetadataHelper.getMetadataFactory().refresh();
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
        List<String> ixs = Arrays.asList(EntityHelper.CreatedOn, EntityHelper.ModifiedOn);
        if (entity.getMainEntity() != null) {
            ixs.add(EntityHelper.QuickCode);
            ixs.add(EntityHelper.OwningUser);
            ixs.add(EntityHelper.OwningDept);
        }
        Table table = new Table40(entity, dialect, ixs);

        String[] ddls = null;
        try {
            ddls = table.generateDDL(false, false, false);
            Application.getSqlExecutor().executeBatch(ddls, DDL_TIMEOUT);
        } catch (Throwable ex) {
            log.error("DDL Error : \n{}", StringUtils.join(ddls, "\n"), ex);
            return false;
        }
        return true;
    }

    /**
     * @return
     */
    protected int genEntityTypeCode() {
        final int starts = 100;

        if (specEntityCode > starts) {
            if (MetadataHelper.containsEntity(specEntityCode)) {
                throw new MetadataException("The `specEntityCode` already exists : " + specEntityCode);
            }
            return specEntityCode;
        }

        // etc. 100,200,300
        int s = CommandArgs.getInt(CommandArgs._StartEntityTypeCode);
        if (s > starts) {
            for (int typeCode = s; typeCode < 999; typeCode++) {
                if (!MetadataHelper.containsEntity(typeCode)) return typeCode;
            }
        }

        Object[] mTypeCode = Application.createQueryNoFilter(
                "select min(typeCode) from MetaEntity").unique();
        int typeCode = mTypeCode == null || ObjectUtils.toInt(mTypeCode[0]) == 0
                ? 999 : (ObjectUtils.toInt(mTypeCode[0]) - 1);

        // 从 999 开始继续利用
        if (typeCode < starts * 2) {
            for (int c = 999; c > starts * 2; c--) {
                if (!MetadataHelper.containsEntity(c)) {
                    log.warn("Recycling and reuse type-code : {}", c);
                    return c;
                }
            }
        }
        return typeCode;
    }
}
