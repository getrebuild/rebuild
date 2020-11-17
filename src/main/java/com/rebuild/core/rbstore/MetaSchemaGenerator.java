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
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.JSONUtils;
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

    final private Entity mainEntity;

    /**
     * @param entity
     */
    public MetaSchemaGenerator(Entity entity) {
        this.mainEntity = entity;
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
        JSONObject schema = (JSONObject) performEntity(mainEntity, false);
        if (mainEntity.getDetailEntity() != null) {
            JSON detail = performEntity(mainEntity.getDetailEntity(), true);
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
        EasyEntity easyEntity = EasyMetaFactory.valueOf(entity);
        schemaEntity.put("entity", entity.getName());
        schemaEntity.put("entityIcon", easyEntity.getIcon());
        schemaEntity.put("entityLabel", easyEntity.getLabel());
        if (easyEntity.getComments() != null) {
            schemaEntity.put("comments", easyEntity.getComments());
        }
        schemaEntity.put("nameField", entity.getNameField().getName());
        schemaEntity.put("quickFields", easyEntity.getExtraAttr("quickFields"));

        // 字段
        JSONArray metaFields = new JSONArray();
        for (Field field : entity.getFields()) {
            if (MetadataHelper.isCommonsField(field)
                    || (isDetail && MetadataHelper.getDetailToMainField(entity).equals(field))) {
                continue;
            }
            metaFields.add(performField(field));
        }
        schemaEntity.put("fields", metaFields);

        schemaEntity.put("layouts", performLayouts(entity));

        schemaEntity.put("fillins", performFillins(entity));

        schemaEntity.put("filters", performFilters(entity));

        schemaEntity.put("triggers", performTriggers(entity));

        if (!isDetail) {
            schemaEntity.put("approvals", performApprovals(entity));
        }

        return schemaEntity;
    }

    /**
     * 字段
     *
     * @param field
     * @return
     */
    protected JSON performField(Field field) {
        final JSONObject schemaField = new JSONObject(true);
        final EasyField easyField = EasyMetaFactory.valueOf(field);
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

        if (dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE) {
            schemaField.put("refEntity", field.getReferenceEntity().getName());
        } else if (dt == DisplayType.PICKLIST || dt == DisplayType.MULTISELECT) {
            schemaField.put("items", performPickList(field));
        }

        JSONObject extConfig = easyField.getExtraAttrs(true);
        if (!extConfig.isEmpty()) {
            schemaField.put("extConfig", extConfig);
        }

        return schemaField;
    }

    /**
     * 列表选项
     *
     * @param field
     * @return
     */
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

    /**
     * 布局（每种布局只保留一个（管理员的））
     *
     * @param entity
     * @return
     */
    protected JSON performLayouts(Entity entity) {
        Object[][] array = Application.createQueryNoFilter(
                "select applyType,config from LayoutConfig where belongEntity = ? and createdBy = ?")
                .setParameter(1, entity.getName())
                .setParameter(2, UserService.ADMIN_USER)
                .array();

        JSONObject layouts = new JSONObject();
        for (Object[] o : array) {
            JSONArray config = (JSONArray) parseJSON(o[1]);
            if (config != null && !config.isEmpty()) {
                String type = (String) o[0];
                layouts.put(type, config);
            }
        }
        return layouts;
    }

    /**
     * 表单自动回填
     *
     * @param entity
     * @return
     */
    protected JSON performFillins(Entity entity) {
        Object[][] array = Application.createQueryNoFilter(
                "select belongField,sourceField,targetField,extConfig from AutoFillinConfig where belongEntity = ?")
                .setParameter(1, entity.getName())
                .array();

        JSONArray fillins = new JSONArray();
        for (Object[] o : array) {
            JSON config = JSONUtils.toJSONObject(
                    new String[] { "belongField", "sourceField", "targetField", "extConfig" },
                    new Object[] { o[0], o[1], o[2], parseJSON(o[3]) });
            fillins.add(config);
        }
        return fillins;
    }

    /**
     * 高级过滤
     *
     * @param entity
     * @return
     */
    protected JSON performFilters(Entity entity) {
        Object[][] array = Application.createQueryNoFilter(
                "select filterName,config from FilterConfig where belongEntity = ? and createdBy = ?")
                .setParameter(1, entity.getName())
                .setParameter(2, UserService.ADMIN_USER)
                .array();

        JSONArray filters = new JSONArray();
        for (Object[] o : array) {
            JSONObject filterConfig = (JSONObject) parseJSON(o[1]);
            if (filterConfig == null) continue;

            JSON config = JSONUtils.toJSONObject(
                    new String[] { "filterName", "config" },
                    new Object[] { o[0], filterConfig });
            filters.add(config);
        }
        return filters;
    }

    /**
     * 触发器
     *
     * @param entity
     * @return
     */
    protected JSON performTriggers(Entity entity) {
        Object[][] array = Application.createQueryNoFilter(
                "select when,whenTimer,whenFilter,actionType,actionContent,priority,name from RobotTriggerConfig where belongEntity = ? and isDisabled = 'F'")
                .setParameter(1, entity.getName())
                .array();

        JSONArray triggers = new JSONArray();
        for (Object[] o : array) {
            JSON actionContent = parseJSON(o[4]);
            if (actionContent == null) continue;

            JSON config = JSONUtils.toJSONObject(
                    new String[] { "when", "whenTimer", "whenFilter", "actionType", "actionContent", "priority", "name" },
                    new Object[] { o[0], o[1], parseJSON(o[2]), o[3], actionContent, o[5], o[6] });
            triggers.add(config);
        }
        return triggers;
    }

    /**
     * 审批流程
     *
     * @param entity
     * @return
     */
    protected JSON performApprovals(Entity entity) {
        Object[][] array = Application.createQueryNoFilter(
                "select name,flowDefinition from RobotApprovalConfig where belongEntity = ? and isDisabled = 'F'")
                .setParameter(1, entity.getName())
                .array();

        JSONArray approvals = new JSONArray();
        for (Object[] o : array) {
            JSON flowDefinition = parseJSON(o[1]);
            if (flowDefinition == null) continue;

            JSON config = JSONUtils.toJSONObject(
                    new String[] { "name", "flowDefinition" },
                    new Object[] { o[0], flowDefinition });
            approvals.add(config);
        }
        return approvals;
    }

    private JSON parseJSON(Object content) {
        if (content == null) return null;

        JSON config = (JSON) JSON.parse(content.toString());
        if (config instanceof JSONObject && ((JSONObject) config).isEmpty()) return null;
        else if (config instanceof JSONArray && ((JSONArray) config).isEmpty()) return null;
        else return config;
    }
}
