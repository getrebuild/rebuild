/*!
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
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
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
     * @param entity
     * @param user
     * @param addonsType
     * @return
     */
    public ConfigBean getLayoutOfViewAddons(String entity, ID user, String addonsType) {
        return getLayout(user, entity, addonsType, null);
    }

    /**
     * 显示项
     *
     * @param entity
     * @param user
     * @return
     */
    public JSONObject getViewTab(String entity, ID user) {
        JSONObject vtabs = getViewAddons(entity, user, TYPE_TAB);

        // 添加明细实体
        Entity ifMain = MetadataHelper.getEntity(entity);
        if (ifMain.getDetailEntity() != null) {
            JSONArray tabsFluent = new JSONArray();
            for (Entity de : MetadataSorter.sortDetailEntities(ifMain)) {
                EasyEntity deEasy = EasyMetaFactory.valueOf(de);
                JSONObject deJson = (JSONObject) deEasy.toJSON();
                deJson.put("entity", de.getName() + "." + MetadataHelper.getDetailToMainField(de).getName());
                // 显示位置
                deJson.put("showAt2", BooleanUtils.toBoolean(deEasy.getExtraAttr(EasyEntityConfigProps.DETAILS_SHOWAT2)) ? 2 : 1);
                tabsFluent.add(deJson);
            }

            tabsFluent.fluentAddAll(vtabs.getJSONArray("items"));
            vtabs.put("items", tabsFluent);
        }
        return vtabs;
    }

    /**
     * 获取显示项过滤条件
     *
     * @param entity
     * @return
     */
    public Map<String, JSONObject> getViewTabFilters(String entity) {
        final ConfigBean config = getLayout(UserService.SYSTEM_USER, entity, TYPE_TAB, null);
        if (config == null) return Collections.emptyMap();

        // compatible: v2.2
        JSON configJson = config.getJSON("config");
        if (configJson instanceof JSONArray) {
            configJson = JSONUtils.toJSONObject("items", configJson);
        }

        JSONArray items = ((JSONObject) configJson).getJSONArray("items");
        if (items == null || items.isEmpty()) return Collections.emptyMap();

        Map<String, JSONObject> filters = new HashMap<>();
        for (Object o : items) {
            // compatible: v2.8
            if (!(o instanceof JSONArray)) continue;

            JSONArray item = (JSONArray) o;
            if (item.size() < 3) continue;

            String entityKey = item.getString(0);
            JSONObject filter = item.getJSONObject(2);
            if (filter != null) filters.put(entityKey, filter);
        }

        return filters;
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
        final ConfigBean config = getLayout(user, entity, applyType, null);
        final Permission useAction = TYPE_TAB.equals(applyType) ? BizzPermission.READ : BizzPermission.CREATE;

        final Entity entityMeta = MetadataHelper.getEntity(entity);
        final Set<Entity> mfRefs = hasMultiFieldsReferenceTo(entityMeta);

        // 未配置则使用全部
        if (config == null) {
            JSONArray useRefs = new JSONArray();
            for (Field field : entityMeta.getReferenceToFields(Boolean.FALSE, Boolean.TRUE)) {
                Entity e = field.getOwnEntity();
                if (!MetadataHelper.isBusinessEntity(e)) continue;
                if (ArrayUtils.contains(entityMeta.getDetialEntities(), e)) continue;

                // 新建项排除明细
                if (TYPE_ADD.equals(applyType)) {
                    if (e.getMainEntity() != null) continue;
                }

                Entity eCheck = ObjectUtils.defaultIfNull(e.getMainEntity(), e);
                if (Application.getPrivilegesManager().allow(user, eCheck.getEntityCode(), useAction)) {
                    useRefs.add(formatEntityShow(field, mfRefs, applyType));
                }
            }

            // 跟进（动态）
            useRefs.add(formatEntityShow(
                    MetadataHelper.getField("Feeds", "relatedRecord"), mfRefs, applyType));
            // 任务（项目）
            useRefs.add(formatEntityShow(
                    MetadataHelper.getField("ProjectTask", "relatedRecord"), mfRefs, applyType));
            // 附件
            if (TYPE_TAB.equals(applyType)) {
                useRefs.add(formatEntityShow(
                        MetadataHelper.getField("Attachment", "relatedRecord"), mfRefs, applyType));
            }

            return JSONUtils.toJSONObject("items", useRefs);
        }

        // compatible: v2.2
        JSON configJson = config.getJSON("config");
        if (configJson instanceof JSONArray) {
            configJson = JSONUtils.toJSONObject("items", configJson);
        }

        JSONArray addons = new JSONArray();
        for (Object o : ((JSONObject) configJson).getJSONArray ("items")) {
            String key;
            String label = null;
            // compatible: v2.8
            if (o instanceof JSONArray) {
                key = (String) ((JSONArray) o).get(0);
                label = (String) ((JSONArray) o).get(1);
            } else {
                key = o.toString();
            }

            // Entity.Field (v1.9)
            String[] ef = key.split("\\.");
            if (!MetadataHelper.containsEntity(ef[0])) {
                continue;
            }

            Entity addonEntity = MetadataHelper.getEntity(ef[0]);
            if (ef.length > 1 && !MetadataHelper.checkAndWarnField(addonEntity, ef[1])) {
                continue;
            }

            if (Application.getPrivilegesManager().allow(user, addonEntity.getEntityCode(), useAction)) {
                JSONObject show = ef.length > 1
                        ? formatEntityShow(addonEntity.getField(ef[1]), mfRefs, applyType)
                        : EasyMetaFactory.toJSON(addonEntity);

                if (StringUtils.isNotBlank(label)) {
                    show.put("_entityLabel", show.getString("entityLabel"));
                    show.put("entityLabel", label);
                }
                addons.add(show);
            }
        }

        return JSONUtils.toJSONObject(
                new String[] { "items", "autoExpand", "autoHide", "defaultList" },
                new Object[] { addons,
                        ((JSONObject) configJson).getBooleanValue("autoExpand"),
                        ((JSONObject) configJson).getBooleanValue("autoHide"),
                        ((JSONObject) configJson).getBooleanValue("defaultList")});
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

        for (Field field : entity.getReferenceToFields(Boolean.FALSE, Boolean.TRUE)) {
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
    private JSONObject formatEntityShow(Field field, Set<Entity> mfRefs, String applyType) {
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
