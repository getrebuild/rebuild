/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.AdvFilterService;
import com.rebuild.core.configuration.general.LayoutConfigService;
import com.rebuild.core.configuration.general.PickListService;
import com.rebuild.core.configuration.general.ShareToManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.*;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 元数据模型导入
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @see MetaSchemaGenerator
 * @since 2019/04/28
 */
public class MetaschemaImporter extends HeavyTask<String> {

    final private String fileUrl;
    private JSONObject remoteData;

    private List<Object[]> picklistHolders = new ArrayList<>();

    /**
     * @param fileUrl
     */
    public MetaschemaImporter(String fileUrl) {
        this.fileUrl = fileUrl;
        this.remoteData = null;
    }

    /**
     * @param data
     */
    public MetaschemaImporter(JSONObject data) {
        this.fileUrl = null;
        this.remoteData = data;
    }

    /**
     * 验证导入
     *
     * @return 错误消息，返回 null 表示验证通过
     */
    public String verfiy() {
        this.readyRemoteData();

        String hasError = verfiyEntity(remoteData);
        if (hasError != null) {
            return hasError;
        }

        JSONObject detailData = remoteData.getJSONObject("detail");
        if (detailData == null) detailData = remoteData.getJSONObject("slave");

        if (detailData != null) {
            hasError = verfiyEntity(detailData);
            return hasError;
        }

        return null;
    }

    private String verfiyEntity(JSONObject entity) {
        String entityName = entity.getString("entity");
        if (MetadataHelper.containsEntity(entityName)) {
            return Language.getLang("SomeDuplicate", "EntityName") + " : " + entityName;
        }

        for (Object o : entity.getJSONArray("fields")) {
            JSONObject field = (JSONObject) o;
            if (DisplayType.REFERENCE.name().equalsIgnoreCase(field.getString("displayType"))) {
                String refEntity = field.getString("refEntity");
                if (!entityName.equals(refEntity) && !MetadataHelper.containsEntity(refEntity)) {
                    return Language.getLang("MissRefEntity") + " : " + field.getString("fieldLabel") + " (" + refEntity + ")";
                }
            }
        }
        return null;
    }

    private void readyRemoteData() {
        if (this.remoteData == null) {
            this.remoteData = (JSONObject) RBStore.fetchMetaschema(fileUrl);
        }
    }

    @Override
    protected String exec() {
        this.readyRemoteData();
        setTotal(100);

        String entityName = performEntity(remoteData, null);
        Entity createdEntity = MetadataHelper.getEntity(entityName);
        setCompleted(45);

        JSONObject detailData = remoteData.getJSONObject("detail");
        if (detailData == null) detailData = remoteData.getJSONObject("slave");

        if (detailData != null) {
            try {
                performEntity(detailData, createdEntity.getName());
                setCompleted(90);
            } catch (MetadataException ex) {
                // 出现异常，删除主实体
                new Entity2Schema(this.getUser()).dropEntity(createdEntity, true);

                throw ex;
            }
        }

        for (Object[] picklist : picklistHolders) {
            Field refreshField = (Field) picklist[0];
            refreshField = MetadataHelper.getField(refreshField.getOwnEntity().getName(), refreshField.getName());
            Application.getBean(PickListService.class).updateBatch(refreshField, (JSONObject) picklist[1]);
        }
        setCompleted(100);

        return entityName;
    }

    /**
     * @param schemaEntity
     * @param mainEntityName
     * @return
     * @throws MetadataException
     */
    private String performEntity(JSONObject schemaEntity, String mainEntityName) throws MetadataException {
        String entityName = schemaEntity.getString("entity");
        String entityLabel = schemaEntity.getString("entityLabel");

        Entity2Schema entity2Schema = new Entity2Schema(this.getUser());
        entity2Schema.createEntity(
                entityName, entityLabel, schemaEntity.getString("comments"), mainEntityName, false);
        Entity entity = MetadataHelper.getEntity(entityName);
        this.setCompleted((int) (this.getCompleted() * 1.5));

        JSONArray fields = schemaEntity.getJSONArray("fields");
        try {
            List<Field> fieldsList = new ArrayList<>();
            for (Object field : fields) {
                Field unsafe = performField((JSONObject) field, entity);
                fieldsList.add(unsafe);
            }

            // 同步字段到数据库
            new Field2Schema(UserService.ADMIN_USER).schema2Database(entity, fieldsList.toArray(new Field[0]));

        } catch (Exception ex) {
            entity2Schema.dropEntity(entity, true);

            if (ex instanceof MetadataException) {
                throw ex;
            } else {
                throw new MetadataException(ex);
            }
        }

        Record needUpdate = EntityHelper.forUpdate(EasyMeta.valueOf(entity).getMetaId(), this.getUser(), false);

        String nameField = schemaEntity.getString("nameField");
        if (nameField != null) {
            needUpdate.setString("nameField", nameField);
        }
        String entityIcon = schemaEntity.getString("entityIcon");
        if (entityIcon != null) {
            needUpdate.setString("icon", entityIcon);
        }
        String quickFields = schemaEntity.getString("quickFields");
        if (quickFields != null) {
            needUpdate.setString("extConfig", JSONUtils.toJSONObject("quickFields", quickFields).toJSONString());
        }

        if (needUpdate.getAvailableFieldIterator().hasNext()) {
            Application.getCommonsService().update(needUpdate);
        }

        // 布局

        JSONObject layouts = schemaEntity.getJSONObject("layouts");
        if (layouts != null) {
            for (Map.Entry<String, Object> e : layouts.entrySet()) {
                performLayout(entityName, e.getKey(), (JSON) e.getValue());
            }
        }

        // 高级查询

        JSONObject filters = schemaEntity.getJSONObject("filters");
        if (filters != null) {
            for (Map.Entry<String, Object> e : filters.entrySet()) {
                performFilter(entityName, e.getKey(), (JSON) e.getValue());
            }
        }

        MetadataHelper.getMetadataFactory().refresh(false);
        return entityName;
    }

    private Field performField(JSONObject schemaField, Entity belong) {
        String fieldName = schemaField.getString("field");
        String fieldLabel = schemaField.getString("fieldLabel");
        String displayType = schemaField.getString("displayType");
        JSON extConfig = schemaField.getJSONObject("extConfig");

        DisplayType dt = DisplayType.valueOf(displayType);
        Field unsafeField = new Field2Schema(this.getUser()).createUnsafeField(
                belong, fieldName, fieldLabel, dt,
                schemaField.getBooleanValue("nullable"),
                true,
                schemaField.getBooleanValue("updatable"),
                !schemaField.containsKey("repeatable") || schemaField.getBooleanValue("repeatable"),
                !schemaField.containsKey("queryable") || schemaField.getBooleanValue("queryable"),
                schemaField.getString("comments"),
                schemaField.getString("refEntity"),
                null,
                extConfig,
                schemaField.getString("defaultValue"));

        if (DisplayType.PICKLIST == dt || DisplayType.MULTISELECT == dt) {
            picklistHolders.add(new Object[]{unsafeField, readyPickList(schemaField.getJSONArray("items"))});
        }

        return unsafeField;
    }

    private JSONObject readyPickList(JSONArray items) {
        JSONArray show = new JSONArray();
        for (Object o : items) {
            JSONArray item = (JSONArray) o;

            JSONObject opt = JSONUtils.toJSONObject(
                    new String[]{"text", "default"},
                    new Object[]{item.getString(0), item.getBoolean(1)});

            // MultiSelect
            if (item.size() > 2) {
                opt.put("mask", item.getLongValue(2));
            }
            show.add(opt);
        }

        return JSONUtils.toJSONObject("show", show);
    }

    private void performLayout(String entity, String type, JSON config) {
        Record record = EntityHelper.forNew(EntityHelper.LayoutConfig, getUser());
        record.setString("belongEntity", entity);
        record.setString("applyType", type);
        record.setString("config", config.toJSONString());
        record.setString("shareTo", ShareToManager.SHARE_ALL);
        Application.getBean(LayoutConfigService.class).create(record);
    }

    private void performFilter(String entity, String filterName, JSON config) {
        Record record = EntityHelper.forNew(EntityHelper.FilterConfig, getUser());
        record.setString("belongEntity", entity);
        record.setString("filterName", filterName);
        record.setString("config", config.toJSONString());
        record.setString("shareTo", ShareToManager.SHARE_ALL);
        Application.getBean(AdvFilterService.class).create(record);
    }
}
