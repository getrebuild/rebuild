/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 元数据辅助类，注意此类返回的数据会过滤和排序
 *
 * @author devezhao
 * @see EasyMetaFactory
 * @see MetadataHelper
 * @since 09/30/2018
 */
public class MetadataSorter {

    /**
     * 全部实体
     *
     * @return
     */
    public static Entity[] sortEntities() {
        return sortEntities(null, true, true);
    }

    /**
     * 用户权限内*可读*实体
     *
     * @param user
     * @param usesBizz 是否包括内置 BIZZ 实体
     * @param usesDetail 是否包括明细实体
     * @return
     */
    public static Entity[] sortEntities(ID user, boolean usesBizz, boolean usesDetail) {
        List<BaseMeta> entities = new ArrayList<>();
        for (Entity e : MetadataHelper.getEntities()) {
            if (!e.isQueryable()) continue;
            if (!usesDetail && e.getMainEntity() != null) continue;

            EasyEntity easyEntity = EasyMetaFactory.valueOf(e);
            if (easyEntity.isBuiltin() && !easyEntity.isPlainEntity()) continue;

            Entity checkEntity = e;
            if (usesDetail && e.getMainEntity() != null) checkEntity = e.getMainEntity();

            if (user == null || !MetadataHelper.hasPrivilegesField(checkEntity)) {
                entities.add(e);
            } else if (Application.getPrivilegesManager().allowRead(user, checkEntity.getEntityCode())) {
                entities.add(e);
            }
        }

        if (usesBizz) {
            entities.add(MetadataHelper.getEntity(EntityHelper.User));
            entities.add(MetadataHelper.getEntity(EntityHelper.Department));
            entities.add(MetadataHelper.getEntity(EntityHelper.Role));
            entities.add(MetadataHelper.getEntity(EntityHelper.Team));
        }

        sortByLabel(entities);
        return entities.toArray(new Entity[0]);
    }

    /**
     * 获取字段
     *
     * @param entity
     * @param usesTypes 仅返回指定的类型
     * @return
     */
    public static Field[] sortFields(Entity entity, DisplayType... usesTypes) {
        return sortFields(entity.getFields(), usesTypes);
    }

    /**
     * 获取字段
     *
     * @param fields
     * @param usesTypes 仅返回指定的类型
     * @return
     */
    public static Field[] sortFields(Field[] fields, DisplayType... usesTypes) {
        List<BaseMeta> fieldsList = new ArrayList<>();
        for (Field field : fields) {
            if (MetadataHelper.isSystemField(field)) continue;

            if (usesTypes.length == 0) {
                fieldsList.add(field);
            } else {
                DisplayType dt = EasyMetaFactory.getDisplayType(field);
                for (DisplayType use : usesTypes) {
                    if (dt == use) fieldsList.add(field);
                }
            }
        }

        if (usesTypes.length == 0) {
            return sortByType(fieldsList);
        } else {
            sortByLabel(fieldsList);
            return fieldsList.toArray(new Field[0]);
        }
    }

    /**
     * 字段排序
     *
     * @param fields
     */
    static Field[] sortByType(List<BaseMeta> fields) {
        List<BaseMeta> othersFields = new ArrayList<>();
        List<BaseMeta> commonsFields = new ArrayList<>();
        List<BaseMeta> approvalFields = new ArrayList<>();

        for (BaseMeta field : fields) {
            if (MetadataHelper.isApprovalField(field.getName())) {
                approvalFields.add(field);
            } else if (MetadataHelper.isCommonsField((Field) field)) {
                commonsFields.add(field);
            } else {
                othersFields.add(field);
            }
        }

        sortByLabel(othersFields);
        List<BaseMeta> allFields = new ArrayList<>(othersFields);

        sortByLabel(commonsFields);
        allFields.addAll(commonsFields);

        sortByLabel(approvalFields);
        allFields.addAll(approvalFields);

        return allFields.toArray(new Field[0]);
    }

    /**
     * 按照显示名排序
     *
     * @param metas
     */
    static void sortByLabel(List<BaseMeta> metas) {
        Comparator<Object> comparator = Collator.getInstance(Locale.CHINESE);
        metas.sort((foo, bar) -> {
            String fooLetter = EasyMetaFactory.getLabel(foo);
            String barLetter = EasyMetaFactory.getLabel(bar);
            return comparator.compare(fooLetter, barLetter);
        });
    }
}
