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
import com.rebuild.core.configuration.general.*;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.EntityRecordCreator;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.RecordBuilder;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.DynamicMetadataContextHolder;
import com.rebuild.core.metadata.impl.Entity2Schema;
import com.rebuild.core.metadata.impl.Field2Schema;
import com.rebuild.core.metadata.impl.MetadataModificationException;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.approval.RobotApprovalConfigService;
import com.rebuild.core.service.trigger.RobotTriggerConfigService;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rebuild.core.support.i18n.Language.$L;

/**
 * 元数据模型导入
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @see MetaSchemaGenerator
 * @since 2019/04/28
 */
public class MetaschemaImporter extends HeavyTask<String> {

    private JSONObject data;

    private List<Object[]> picklistHolders = new ArrayList<>();

    private boolean needClearContextHolder = false;

    /**
     * @param data
     */
    public MetaschemaImporter(JSONObject data) {
        this.data = data;
    }

    /**
     * 验证导入文件
     *
     * @return 错误消息，返回 null 表示验证通过
     */
    public String verfiy() {
        String hasError = verfiyEntity(data);
        if (hasError != null) {
            return hasError;
        }

        JSONObject detailData = data.getJSONObject("detail");
        if (detailData == null) detailData = data.getJSONObject("slave");

        if (detailData != null) {
            hasError = verfiyEntity(detailData);
            return hasError;
        }

        return null;
    }

    private String verfiyEntity(JSONObject entity) {
        String entityName = entity.getString("entity");
        if (MetadataHelper.containsEntity(entityName)) {
            return $L("实体名称已存在 : %s",entityName);
        }

        for (Object o : entity.getJSONArray("fields")) {
            JSONObject field = (JSONObject) o;
            String dt = field.getString("displayType");
            if (DisplayType.REFERENCE.name().equals(dt) || DisplayType.N2NREFERENCE.name().equals(dt)) {
                String refEntity = field.getString("refEntity");
                if (!entityName.equals(refEntity) && !MetadataHelper.containsEntity(refEntity)) {
                    return $L("缺少必要的引用实体 : %s (%s)", field.getString("fieldLabel"), refEntity);
                }
            }
        }
        return null;
    }

    /**
     * 执行导入
     *
     * @return
     */
    @Override
    protected String exec() {
        setTotal(100);

        if (!DynamicMetadataContextHolder.isSkipLanguageRefresh(false)) {
            DynamicMetadataContextHolder.setSkipLanguageRefresh();
            needClearContextHolder = true;
        }

        String entityName = performEntity(data, null);
        Entity createdEntity = MetadataHelper.getEntity(entityName);
        setCompleted(45);

        JSONObject detailData = data.getJSONObject("detail");
        if (detailData == null) detailData = data.getJSONObject("slave");

        if (detailData != null) {
            try {
                performEntity(detailData, createdEntity.getName());
                setCompleted(90);
            } catch (MetadataModificationException ex) {
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

    @Override
    protected void completedAfter() {
        super.completedAfter();

        if (needClearContextHolder) {
            DynamicMetadataContextHolder.isSkipLanguageRefresh(true);
        }
    }

    /**
     * @param schemaEntity
     * @param mainEntityName
     * @return
     * @throws MetadataModificationException
     */
    private String performEntity(JSONObject schemaEntity, String mainEntityName) throws MetadataModificationException {
        final String entityName = schemaEntity.getString("entity");
        final String entityLabel = schemaEntity.getString("entityLabel");

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
                if (unsafe != null) fieldsList.add(unsafe);
            }

            // 同步字段到数据库
            new Field2Schema(UserService.ADMIN_USER).schema2Database(entity, fieldsList.toArray(new Field[0]));

        } catch (Exception ex) {
            entity2Schema.dropEntity(entity, true);

            if (ex instanceof MetadataModificationException) {
                throw ex;
            } else {
                throw new MetadataModificationException(ex);
            }
        }

        Record needUpdate = EntityHelper.forUpdate(EasyMetaFactory.valueOf(entity).getMetaId(), this.getUser(), false);

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

        // 刷新元数据
        MetadataHelper.getMetadataFactory().refresh(false);

        // 布局
        JSONObject layouts = schemaEntity.getJSONObject("layouts");
        if (layouts != null) {
            for (Map.Entry<String, Object> e : layouts.entrySet()) {
                performLayout(entityName, e.getKey(), (JSON) e.getValue());
            }
        }

        // 表单回填
        JSONArray fillins = schemaEntity.getJSONArray("fillins");
        if (fillins != null) {
            for (Object o : fillins) {
                performFillin(entityName, (JSONObject) o);
            }
        }

        // 高级查询
        JSONArray filters = schemaEntity.getJSONArray("filters");
        if (filters != null) {
            for (Object o : filters) {
                performFilter(entityName, (JSONObject) o);
            }
        }

        // 触发器
        JSONArray triggers = schemaEntity.getJSONArray("triggers");
        if (triggers != null) {
            for (Object o : triggers) {
                performTrigger(entityName, (JSONObject) o);
            }
        }

        // 审批流程
        JSONArray approvals = schemaEntity.getJSONArray("approvals");
        if (approvals != null) {
            for (Object o : approvals) {
                performApproval(entityName, (JSONObject) o);
            }
        }

        // 记录转换
        JSONArray transforms = schemaEntity.getJSONArray("transforms");
        if (transforms != null) {
            for (Object o : transforms) {
                performTransform(entityName, (JSONObject) o);
            }
        }

        return entityName;
    }

    private Field performField(JSONObject schemaField, Entity belong) {
        String fieldName = schemaField.getString("field");
        String fieldLabel = schemaField.getString("fieldLabel");
        String displayType = schemaField.getString("displayType");
        JSON extConfig = schemaField.getJSONObject("extConfig");

        DisplayType dt = DisplayType.valueOf(displayType);
        if (dt == DisplayType.ID || MetadataHelper.isCommonsField(fieldName)) {
            return null;
        }

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
            picklistHolders.add(new Object[] {
                    unsafeField, performPickList(schemaField.getJSONArray("items")) });
        }

        return unsafeField;
    }

    private JSONObject performPickList(JSONArray items) {
        JSONArray shown = new JSONArray();
        for (Object o : items) {
            JSONArray item = (JSONArray) o;

            JSONObject option = JSONUtils.toJSONObject(
                    new String[] { "text", "default" },
                    new Object[] { item.getString(0), item.getBoolean(1) });
            // MultiSelect
            if (item.size() > 2) {
                option.put("mask", item.getLongValue(2));
            }

            shown.add(option);
        }

        return JSONUtils.toJSONObject("show", shown);
    }

    private void performLayout(String entity, String applyType, JSON config) {
        Record record = RecordBuilder.builder(EntityHelper.LayoutConfig)
                .add("belongEntity", entity)
                .add("applyType", applyType)
                .add("config", config.toJSONString())
                .add("shareTo", ShareToManager.SHARE_ALL)
                .build(getUser());
        Application.getBean(LayoutConfigService.class).create(record);
    }

    private void performFillin(String entity, JSONObject config) {
        Entity configEntity = MetadataHelper.getEntity(EntityHelper.AutoFillinConfig);
        config.put("metadata", JSONUtils.toJSONObject("entity", configEntity.getName()));
        config.put("belongEntity", entity);

        Record record = new EntityRecordCreator(configEntity, config, getUser())
                .create();
        Application.getBean(AutoFillinConfigService.class).create(record);
    }

    private void performFilter(String entity, JSONObject config) {
        Entity configEntity = MetadataHelper.getEntity(EntityHelper.FilterConfig);
        config.put("metadata", JSONUtils.toJSONObject("entity", configEntity.getName()));
        config.put("belongEntity", entity);
        config.put("shareTo", ShareToManager.SHARE_ALL);

        Record record = new EntityRecordCreator(configEntity, config, getUser())
                .create();
        Application.getBean(AdvFilterService.class).create(record);
    }

    private void performTrigger(String entity,JSONObject config) {
        Entity configEntity = MetadataHelper.getEntity(EntityHelper.RobotTriggerConfig);
        config.put("metadata", JSONUtils.toJSONObject("entity", configEntity.getName()));
        config.put("belongEntity", entity);

        Record record = new EntityRecordCreator(configEntity, config, getUser())
                .create();
        Application.getBean(RobotTriggerConfigService.class).create(record);
    }

    private void performApproval(String entity, JSONObject config) {
        Entity configEntity = MetadataHelper.getEntity(EntityHelper.RobotApprovalConfig);
        config.put("metadata", JSONUtils.toJSONObject("entity", configEntity.getName()));
        config.put("belongEntity", entity);

        Record record = new EntityRecordCreator(configEntity, config, getUser())
                .create();
        Application.getBean(RobotApprovalConfigService.class).create(record);
    }

    private void performTransform(String entity, JSONObject config) {
        Entity configEntity = MetadataHelper.getEntity(EntityHelper.TransformConfig);
        config.put("metadata", JSONUtils.toJSONObject("entity", configEntity.getName()));
        config.put("belongEntity", entity);

        Record record = new EntityRecordCreator(configEntity, config, getUser())
                .create();
        Application.getBean(TransformConfigService.class).create(record);
    }
}
