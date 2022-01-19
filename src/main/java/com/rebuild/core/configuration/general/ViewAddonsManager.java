/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 视图-相关项/新建相关
 *
 * @author devezhao
 * @since 10/22/2018
 */
public class ViewAddonsManager extends BaseLayoutManager {

    public static final ViewAddonsManager instance = new ViewAddonsManager();

    private ViewAddonsManager() {
    }

    /**
     * A实体中可能引用了两次B实体（2个引用字段都引用B），因此在设置相关项时必须包含哪个字段（Entity.Field）
     * 在 v1.9 之前上述场景存在问题
     */
    public static final String EF_SPLIT = ".";

    /**
     * 显示项
     *
     * @param entity
     * @param user
     * @return
     */
    public JSONObject getViewTab(String entity, ID user) {
        JSONObject vtabs = getViewAddons(entity, user, TYPE_TAB);

        // 添加明细实体到第一个
        Entity entityMeta = MetadataHelper.getEntity(entity);
        Entity detailMeta;
        if ((detailMeta = entityMeta.getDetailEntity()) != null) {
            JSONObject detail = EasyMetaFactory.toJSON(detailMeta);
            detail.put("entity", detailMeta.getName() + "." + MetadataHelper.getDetailToMainField(detailMeta).getName());
            JSONArray tabsFluent = new JSONArray();
            tabsFluent.add(detail);
            tabsFluent.fluentAddAll(vtabs.getJSONArray("items"));
            vtabs.put("items", tabsFluent);
        }
        return vtabs;
    }

    /**
     * 新建项
     *
     * @param entity
     * @param user
     * @return
     */
    public JSONObject getViewAdd(String entity, ID user) {
        return getViewAddons(entity, user, TYPE_ADD);
    }

    /**
     * @param entity
     * @param user
     * @param applyType
     * @return
     */
    private JSONObject getViewAddons(String entity, ID user, String applyType) {
        final ConfigBean config = getLayout(user, entity, applyType);
        final Permission useAction = TYPE_TAB.equals(applyType) ? BizzPermission.READ : BizzPermission.CREATE;

        final Entity entityMeta = MetadataHelper.getEntity(entity);
        final Set<Entity> mfRefs = hasMultiFieldsReferenceTo(entityMeta);

        // 未配置则使用全部
        if (config == null) {
            JSONArray useRefs = new JSONArray();
            for (Field field : entityMeta.getReferenceToFields(true)) {
                Entity e = field.getOwnEntity();
                if (e.getMainEntity() == null &&
                        Application.getPrivilegesManager().allow(user, e.getEntityCode(), useAction)) {
                    useRefs.add(getEntityShow(field, mfRefs, applyType));
                }
            }

            // 跟进（动态）
            useRefs.add(getEntityShow(
                    MetadataHelper.getField("Feeds", "relatedRecord"), mfRefs, applyType));
            // 任务（项目）
            useRefs.add(getEntityShow(
                    MetadataHelper.getField("ProjectTask", "relatedRecord"), mfRefs, applyType));
            // 附件
            if (TYPE_TAB.equals(applyType)) {
                useRefs.add(getEntityShow(
                        MetadataHelper.getField("Attachment", "relatedRecord"), mfRefs, applyType));
            }

            return JSONUtils.toJSONObject("items", useRefs);
        }

        // fix: v2.2 兼容
        JSON configJson = config.getJSON("config");
        if (configJson instanceof JSONArray) {
            configJson = JSONUtils.toJSONObject("items", configJson);
        }

        JSONArray addons = new JSONArray();
        for (Object o : ((JSONObject) configJson).getJSONArray ("items")) {
            // Entity.Field (v1.9)
            String[] e = ((String) o).split("\\.");
            if (!MetadataHelper.containsEntity(e[0])) {
                continue;
            }

            Entity addonEntity = MetadataHelper.getEntity(e[0]);
            if (e.length > 1 && !MetadataHelper.checkAndWarnField(addonEntity, e[1])) {
                continue;
            }

            if (Application.getPrivilegesManager().allow(user, addonEntity.getEntityCode(), useAction)) {
                if (e.length > 1) {
                    addons.add(getEntityShow(addonEntity.getField(e[1]), mfRefs, applyType));
                } else {
                    addons.add(EasyMetaFactory.toJSON(addonEntity));
                }
            }
        }

        return JSONUtils.toJSONObject(
                new String[] { "items", "autoExpand", "autoHide" },
                new Object[] { addons,
                        ((JSONObject) configJson).getBooleanValue("autoExpand"),
                        ((JSONObject) configJson).getBooleanValue("autoHide") });
    }

    /**
     * 同一实体的多个字段引用同一个实体
     *
     * @param entity
     * @return
     */
    public static Set<Entity> hasMultiFieldsReferenceTo(Entity entity) {
        Map<Entity, Integer> map = new HashMap<>();
        map.put(entity, 1);  // 包括自己

        for (Field field : entity.getReferenceToFields(true)) {
            Entity e = field.getOwnEntity();
            // 排除明细实体
            if (e.getMainEntity() == null) {
                int t = map.getOrDefault(e, 0);
                map.put(e, t + 1);
            }
        }

        Set<Entity> set = new HashSet<>();
        for (Map.Entry<Entity, Integer> e : map.entrySet()) {
            if (e.getValue() > 1) {
                set.add(e.getKey());
            }
        }
        return set;
    }

    /**
     * @see EasyMetaFactory#toJSON(Entity)
     */
    private JSONObject getEntityShow(Field field, Set<Entity> mfRefs, String applyType) {
        Entity fieldEntity = field.getOwnEntity();
        JSONObject show = EasyMetaFactory.toJSON(fieldEntity);
        show.put("entity", fieldEntity.getName() + EF_SPLIT + field.getName());

        if (mfRefs.contains(fieldEntity)) {
            String entityLabel = TYPE_TAB.equalsIgnoreCase(applyType)
                    ? String.format("%s (%s)", EasyMetaFactory.getLabel(field), show.getString("entityLabel"))
                    : String.format("%s (%s)", show.getString("entityLabel"), EasyMetaFactory.getLabel(field));
            show.put("entityLabel", entityLabel);
        } else if (fieldEntity.getEntityCode() == EntityHelper.Feeds) {
            show.put("entityLabel", Language.L("动态"));
        }
        return show;
    }
}
