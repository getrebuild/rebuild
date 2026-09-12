/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 获取系统实体元数据信息
 *
 * @author devezhao
 * @since 2026/7/10
 */
@Slf4j
public class ListEntities implements Tool {

    // 对 AI 无用的字段属性，移除以节省 token，其余属性一律保留
    private static final String[] FIELD_NOISE_PROPS = {"queryable", "repeatable", "ref", "stateClass"};

    private static final String RELATIONS_MESSAGE = "以上为当前用户可见的全部实体及其引用关系，"
            + "凡涉及实体之间关联的分析、查询、绘图、配置均可直接据此进行，无需再逐个查询实体";

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String name = args.getString("name");
        if (StringUtils.isNotBlank(name)) return getEntityMeta(name);

        return listEntities(args.getBooleanValue("relations"));
    }

    /**
     * 获取指定实体的元数据（含字段定义），支持逗号分隔一次查询多个
     *
     * @param entityIdent
     * @return
     */
    private JSONObject getEntityMeta(String entityIdent) {
        String[] idents = StringUtils.split(entityIdent, ",，;；");

        if (idents.length == 1) {
            return JSONUtils.toJSONObject(
                    new String[]{"status", "entity"},
                    new Object[]{"ok", buildEntityMeta(idents[0])});
        }

        JSONArray entities = new JSONArray();
        for (String ident : idents) entities.add(buildEntityMeta(ident));
        return JSONUtils.toJSONObject(
                new String[]{"status", "entities"},
                new Object[]{"ok", entities});
    }

    /**
     * 构建单个实体的元数据
     *
     * @param entityIdent
     * @return
     */
    private JSONObject buildEntityMeta(String entityIdent) {
        entityIdent = entityIdent.trim();

        Entity entity = ToolHelper.resolveEntity(entityIdent);
        if (entity == null) {
            throw new KnownToolException("未知实体 : " + entityIdent + ToolHelper.suggestEntity(entityIdent));
        }

        // 口径与 listEntities 的过滤保持一致，否则非管理员可绕过列表直接取得组织实体的全部字段定义
        if (!isEntityVisible(entity, UserHelper.isAdmin(UserContextHolder.getUser()))) {
            throw new KnownToolException("实体 [" + entityIdent + "] 为系统/组织实体，仅管理员可查看其字段定义");
        }

        JSONObject entityJson = EasyMetaFactory.toJSON(entity);
        entityJson.put("name", entity.getName());
        entityJson.put("label", EasyMetaFactory.getLabel(entity));

        JSONArray fields = new JSONArray();
        JSONArray commonsFields = new JSONArray();
        for (Field field : entity.getFields()) {
            if (MetadataHelper.isSystemField(field)) continue;

            // 公共字段（创建人/时间、所属用户/部门、审批相关）各实体结构一致，只列名不展开定义
            if (MetadataHelper.isCommonsField(field)) {
                commonsFields.add(field.getName());
                continue;
            }

            JSONObject fieldJson = EasyMetaFactory.toJSON(field);
            for (String prop : FIELD_NOISE_PROPS) fieldJson.remove(prop);
            fieldJson.put("name", field.getName());
            fieldJson.put("label", EasyMetaFactory.getLabel(field));
            DisplayType dt = EasyMetaFactory.valueOf(field).getDisplayType();
            fieldJson.put("type", dt.name());

            if (field.getType() == FieldType.REFERENCE || field.getType() == FieldType.REFERENCE_LIST) {
                Entity refEntity = field.getReferenceEntity();
                fieldJson.put("referenceEntity", refEntity.getName());
            }

            fields.add(fieldJson);
        }
        entityJson.put("fields", fields);
        if (!commonsFields.isEmpty()) entityJson.put("commonsFields", commonsFields);

        Entity mainEntity = entity.getMainEntity();
        if (mainEntity != null) {
            entityJson.put("mainEntity", mainEntity.getName());
        }

        if (entity.getDetailEntity() != null) {
            JSONArray details = new JSONArray();
            for (Entity de : MetadataSorter.sortDetailEntities(entity)) {
                JSONObject deJson = EasyMetaFactory.toJSON(de);
                deJson.put("name", de.getName());
                deJson.put("label", EasyMetaFactory.getLabel(de));
                details.add(deJson);
            }
            entityJson.put("detailEntities", details);
        }

        return entityJson;
    }

    /**
     * 列出所有业务实体（管理员额外返回用户/部门/角色/团队等组织实体）
     *
     * @param relations 是否一并返回实体间的引用关系
     * @return
     */
    private JSONObject listEntities(boolean relations) {
        boolean isAdmin = UserHelper.isAdmin(UserContextHolder.getUser());

        JSONArray list = new JSONArray();
        for (Entity e : MetadataHelper.getEntities()) {
            if (!isEntityVisible(e, isAdmin)) continue;
            if (e.getMainEntity() != null) continue;

            JSONObject item = new JSONObject(true);
            item.put("name", e.getName());
            item.put("label", EasyMetaFactory.getLabel(e));
            item.put("comments", StringUtils.defaultIfBlank(EasyMetaFactory.valueOf(e).getComments(), ""));
            if (relations) item.put("refs", buildRefs(e));

            if (e.getDetailEntity() != null) {
                JSONArray details = new JSONArray();
                for (Entity de : MetadataSorter.sortDetailEntities(e)) {
                    JSONObject deItem = new JSONObject(true);
                    deItem.put("name", de.getName());
                    deItem.put("label", EasyMetaFactory.getLabel(de));
                    deItem.put("comments", StringUtils.defaultIfBlank(EasyMetaFactory.valueOf(de).getComments(), ""));
                    if (relations) deItem.put("refs", buildRefs(de));
                    details.add(deItem);
                }
                item.put("detailEntities", details);
            }

            list.add(item);
        }

        if (relations) {
            return JSONUtils.toJSONObject(
                    new String[]{"status", "entities", "message"},
                    new Object[]{"ok", list, RELATIONS_MESSAGE});
        }
        return JSONUtils.toJSONObject(
                new String[]{"status", "entities"},
                new Object[]{"ok", list});
    }

    /**
     * 实体的引用关系（字段名 → 被引用实体名），不含创建人、所属部门、审批等公共字段
     *
     * @param entity
     * @return
     */
    private static JSONObject buildRefs(Entity entity) {
        JSONObject refs = new JSONObject(true);
        for (Field field : entity.getFields()) {
            if (MetadataHelper.isCommonsField(field)) continue;
            if (field.getType() != FieldType.REFERENCE && field.getType() != FieldType.REFERENCE_LIST) continue;

            Entity refEntity = field.getReferenceEntity();
            if (refEntity != null) refs.put(field.getName(), refEntity.getName());
        }
        return refs;
    }

    /**
     * 实体是否对当前用户可见。业务实体全部可见，用户/部门/角色/团队等组织实体仅管理员可见
     *
     * @param e
     * @param isAdmin
     * @return
     */
    private static boolean isEntityVisible(Entity e, boolean isAdmin) {
        return MetadataHelper.isBusinessEntity(e) || (isAdmin && MetadataHelper.isBizzEntity(e));
    }
}
