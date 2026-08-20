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

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String name = args.getString("name");
        if (StringUtils.isNotBlank(name)) return getEntityMeta(name);

        return listEntities();
    }

    /**
     * 获取指定实体的元数据（含字段定义）
     *
     * @param entityIdent
     * @return
     */
    private JSONObject getEntityMeta(String entityIdent) {
        Entity entity = ToolHelper.resolveEntity(entityIdent);
        if (entity == null) {
            throw new KnownToolException("未知实体 : " + entityIdent + ToolHelper.suggestEntity(entityIdent));
        }

        JSONObject entityJson = EasyMetaFactory.toJSON(entity);
        entityJson.put("name", entity.getName());
        entityJson.put("label", EasyMetaFactory.getLabel(entity));

        JSONArray fields = new JSONArray();
        for (Field field : entity.getFields()) {
            if (MetadataHelper.isSystemField(field)) continue;

            JSONObject fieldJson = EasyMetaFactory.toJSON(field);
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

        return JSONUtils.toJSONObject(
                new String[]{"status", "entity"},
                new Object[]{"ok", entityJson});
    }

    /**
     * 列出所有业务实体（管理员额外返回用户/部门/角色/团队等组织实体）
     *
     * @return
     */
    private JSONObject listEntities() {
        boolean isAdmin = UserHelper.isAdmin(UserContextHolder.getUser());

        JSONArray list = new JSONArray();
        for (Entity e : MetadataHelper.getEntities()) {
            if (!MetadataHelper.isBusinessEntity(e) && !(isAdmin && MetadataHelper.isBizzEntity(e))) continue;
            if (e.getMainEntity() != null) continue;

            JSONObject item = new JSONObject();
            item.put("name", e.getName());
            item.put("label", EasyMetaFactory.getLabel(e));
            item.put("comments", StringUtils.defaultIfBlank(EasyMetaFactory.valueOf(e).getComments(), ""));

            if (e.getDetailEntity() != null) {
                JSONArray details = new JSONArray();
                for (Entity de : MetadataSorter.sortDetailEntities(e)) {
                    JSONObject deItem = new JSONObject();
                    deItem.put("name", de.getName());
                    deItem.put("label", EasyMetaFactory.getLabel(de));
                    deItem.put("comments", StringUtils.defaultIfBlank(EasyMetaFactory.valueOf(de).getComments(), ""));
                    details.add(deItem);
                }
                item.put("detailEntities", details);
            }

            list.add(item);
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "entities"},
                new Object[]{"ok", list});
    }
}
