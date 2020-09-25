/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.privileges.UserService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * 元数据模型生成
 *
 * @author devezhao zhaofang123@gmail.com
 * @see MetaschemaImporter
 * @since 2019/04/28
 */
public class MetaSchemaGenerator {

    final private Entity entity;

    /**
     * @param entity
     */
    public MetaSchemaGenerator(Entity entity) {
        this.entity = entity;
    }

    /**
     * @param dest
     * @throws IOException
     */
    public void generate(File dest) throws IOException {
        JSON schema = generate();
        FileUtils.writeStringToFile(dest, JSON.toJSONString(schema, true), "utf-8");
    }

    /**
     * @return
     */
    public JSON generate() {
        JSONObject schema = (JSONObject) performEntity(entity, false);
        if (entity.getDetailEntity() != null) {
            JSON detail = performEntity(entity.getDetailEntity(), true);
            schema.put("detail", detail);
        }
        return schema;
    }

    /**
     * @param entity
     * @param isDetail
     * @return
     */
    private JSON performEntity(Entity entity, boolean isDetail) {
        JSONObject schemaEntity = new JSONObject(true);

        // 实体
        EasyMeta easyEntity = EasyMeta.valueOf(entity);
        schemaEntity.put("entity", entity.getName());
        schemaEntity.put("entityIcon", easyEntity.getIcon());
        schemaEntity.put("entityLabel", easyEntity.getLabel());
        if (easyEntity.getComments() != null) {
            schemaEntity.put("comments", easyEntity.getComments());
        }
        schemaEntity.put("nameField", entity.getNameField().getName());
        schemaEntity.put("quickFields", easyEntity.getExtraAttr("quickFields"));

        JSONArray metaFields = new JSONArray();
        for (Field field : entity.getFields()) {
            if (MetadataHelper.isCommonsField(field)
                    || (isDetail && MetadataHelper.getDetailToMainField(entity).equals(field))) {
                continue;
            }
            metaFields.add(performField(field));
        }
        schemaEntity.put("fields", metaFields);

        // 布局相关（仅管理员的）

        JSONObject putLayouts = new JSONObject();
        Object[][] layouts = Application.createQueryNoFilter(
                "select applyType,config from LayoutConfig where belongEntity = ? and createdBy = ?")
                .setParameter(1, entity.getName())
                .setParameter(2, UserService.ADMIN_USER)
                .array();
        for (Object[] layout : layouts) {
            String type = (String) layout[0];
            JSONArray config = JSON.parseArray((String) layout[1]);
            if (!config.isEmpty()) {
                putLayouts.put(type, config);
            }
        }
        schemaEntity.put("layouts", putLayouts);

        // 过滤器（仅管理员的）

        JSONObject putFilters = new JSONObject();
        Object[][] filters = Application.createQueryNoFilter(
                "select filterName,config from FilterConfig where belongEntity = ? and createdBy = ?")
                .setParameter(1, entity.getName())
                .setParameter(2, UserService.ADMIN_USER)
                .array();
        for (Object[] filter : filters) {
            String name = (String) filter[0];
            JSONObject config = JSON.parseObject((String) filter[1]);
            if (!config.isEmpty()) {
                putFilters.put(name, config);
            }
        }
        schemaEntity.put("filters", putFilters);

        return schemaEntity;
    }

    /**
     * @param field
     * @return
     */
    private JSON performField(Field field) {
        final JSONObject schemaField = new JSONObject(true);
        final EasyMeta easyField = EasyMeta.valueOf(field);
        final DisplayType dt = easyField.getDisplayType();

        schemaField.put("field", easyField.getName());
        schemaField.put("fieldLabel", easyField.getLabel());
        schemaField.put("displayType", dt.name());
        if (easyField.getComments() != null) {
            schemaField.put("comments", easyField.getComments());
        }
        schemaField.put("nullable", field.isNullable());
        schemaField.put("updatable", field.isUpdatable());
        schemaField.put("repeatable", field.isRepeatable());
        schemaField.put("queryable", field.isQueryable());
        Object defaultVal = field.getDefaultValue();
        if (defaultVal != null && StringUtils.isNotBlank((String) defaultVal)) {
            schemaField.put("defaultValue", defaultVal);
        }

        if (dt == DisplayType.REFERENCE) {
            schemaField.put("refEntity", field.getReferenceEntity().getName());
            schemaField.put("refEntityLabel", EasyMeta.getLabel(field.getReferenceEntity()));
        } else if (dt == DisplayType.PICKLIST || dt == DisplayType.MULTISELECT) {
            schemaField.put("items", performPickList(field));
        }

        JSONObject extConfig = easyField.getExtraAttrs(true);
        if (!extConfig.isEmpty()) {
            schemaField.put("extConfig", extConfig);
        }

        return schemaField;
    }

    private JSON performPickList(Field field) {
        ConfigBean[] entries = PickListManager.instance.getPickListRaw(
                field.getOwnEntity().getName(), field.getName(), false);

        JSONArray items = new JSONArray();
        for (ConfigBean e : entries) {
            items.add(new Object[] {
                    e.getString("text"), e.getBoolean("default"), e.getLong("mask")
            });
        }
        return items;
    }
}
