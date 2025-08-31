/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.trigger.TriggerAction;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 元数据辅助类，注意此类返回的数据会过滤和排序
 *
 * @author devezhao
 * @see EasyMetaFactory
 * @see MetadataHelper
 * @since 09/30/2018
 */
@SuppressWarnings("SuspiciousToArrayCall")
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
     * @param includesBizz 是否包括内置 BIZZ 实体
     * @param includesDetail 是否包括明细实体
     * @return
     */
    public static Entity[] sortEntities(ID user, boolean includesBizz, boolean includesDetail) {
        List<BaseMeta> entities = new ArrayList<>();
        for (Entity e : MetadataHelper.getEntities()) {
            if (!e.isQueryable()) continue;
            if (!includesDetail && e.getMainEntity() != null) continue;

            EasyEntity easyEntity = EasyMetaFactory.valueOf(e);
            if (easyEntity.isBuiltin() && !easyEntity.isPlainEntity()) continue;

            Entity checkEntity = e;
            if (includesDetail && e.getMainEntity() != null) checkEntity = e.getMainEntity();

            if (user == null || !MetadataHelper.hasPrivilegesField(checkEntity)) {
                entities.add(e);
            } else if (Application.getPrivilegesManager().allowRead(user, checkEntity.getEntityCode())) {
                entities.add(e);
            }
        }

        sortByLabel(entities);

        // 明细放在主实体后面
        if (includesDetail) {
            List<BaseMeta> entities2 = new ArrayList<>();
            for (BaseMeta o : entities) {
                Entity me = (Entity) o;
                if (me.getDetailEntity() == null && me.getMainEntity() == null) {
                    entities2.add(me);
                } else if (me.getDetailEntity() != null) {
                    entities2.add(me);

                    for (BaseMeta de : entities) {
                        if (me.equals(((Entity) de).getMainEntity())) entities2.add(de);
                    }
                }
            }

            entities = entities2;
        }

        if (includesBizz) {
            entities.add(MetadataHelper.getEntity(EntityHelper.User));
            entities.add(MetadataHelper.getEntity(EntityHelper.Department));
            entities.add(MetadataHelper.getEntity(EntityHelper.Role));
            entities.add(MetadataHelper.getEntity(EntityHelper.Team));
        }

        return entities.toArray(new Entity[0]);
    }

    /**
     * @param mainEntity
     * @return
     */
    public static Entity[] sortDetailEntities(Entity mainEntity) {
        Assert.notNull(mainEntity.getDetailEntity(), "None main entity : " + mainEntity);

        List<BaseMeta> entities = new ArrayList<>();
        CollectionUtils.addAll(entities, mainEntity.getDetialEntities());
        if (entities.size() <= 1) entities.toArray(new Entity[0]);

        // 显示顺序（默认是返回按CODE大小）
        entities.sort((foo, bar) -> {
            String fooSeq = foo.getExtraAttrs().getString("detailsSeq");
            String barSeq = bar.getExtraAttrs().getString("detailsSeq");
            if (StringUtils.isBlank(fooSeq) || StringUtils.isBlank(barSeq)) return 0;
            return fooSeq.compareTo(barSeq);
        });
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

        return sortByLevel(fieldsList);
    }

    /**
     * 按字段分类排序
     *
     * @param fields
     */
    static Field[] sortByLevel(List<BaseMeta> fields) {
        List<BaseMeta> bizFields = new ArrayList<>();
        Map<String, BaseMeta> comFieldsMap = new HashMap<>();

        for (BaseMeta field : fields) {
            if (MetadataHelper.isApprovalField(field.getName())
                    || MetadataHelper.isCommonsField((Field) field)) {
                comFieldsMap.put(field.getName(), field);
            } else {
                bizFields.add(field);
            }
        }

        sortByLabel(bizFields);
        List<BaseMeta> allFields = new ArrayList<>(bizFields);

        // v3.7 特殊排序
        final String[] specSortsApproval = new String[] {
                EntityHelper.ApprovalId, EntityHelper.ApprovalState,
                EntityHelper.ApprovalStepNodeName, EntityHelper.ApprovalStepUsers,
                EntityHelper.ApprovalLastUser, EntityHelper.ApprovalLastTime, EntityHelper.ApprovalLastRemark
        };
        List<BaseMeta> approvalFields = new ArrayList<>();
        for (String s : specSortsApproval) {
            BaseMeta b = comFieldsMap.get(s);
            if (b != null) approvalFields.add(b);
        }
        allFields.addAll(approvalFields);

        // v3.5 特殊排序
        final String[] specSortsCommon = new String[] {
                EntityHelper.CreatedBy, EntityHelper.CreatedOn, EntityHelper.ModifiedBy, EntityHelper.ModifiedOn,
                EntityHelper.OwningUser, EntityHelper.OwningDept
        };
        List<BaseMeta> commonsFields = new ArrayList<>();
        for (String s : specSortsCommon) {
            BaseMeta b = comFieldsMap.get(s);
            if (b != null) commonsFields.add(b);
        }
        allFields.addAll(commonsFields);

        return allFields.toArray(new Field[0]);
    }

    /**
     * 按显示名排序
     *
     * @param metas
     */
    public static void sortByLabel(List<BaseMeta> metas) {
        if (metas.size() <= 1) return;

        Comparator<Object> comparator = Collator.getInstance(Locale.CHINESE);
        metas.sort((foo, bar) -> {
            String fooLetter = EasyMetaFactory.getLabel(foo);
            String barLetter = EasyMetaFactory.getLabel(bar);
            return comparator.compare(fooLetter, barLetter);
        });
    }

    /**
     * 排序
     *
     * @param entities
     * @param selfEntity 添加自己
     */
    public static void sortEntities(List<Object[]> entities, Entity selfEntity) {
        Comparator<Object> comparator = Collator.getInstance(Locale.CHINESE);
        entities.sort((o1, o2) -> comparator.compare(o1[1], o2[1]));

        if (selfEntity != null) {
            entities.add(new String[]{
                    selfEntity.getName(), EasyMetaFactory.getLabel(selfEntity), TriggerAction.SOURCE_SELF});
        }
    }
}
