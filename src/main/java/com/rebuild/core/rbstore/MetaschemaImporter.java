/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.PersistManagerImpl;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
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
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.rebuild.core.rbstore.MetaSchemaGenerator.KEEP_ID;

/**
 * 元数据模型导入
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @see MetaSchemaGenerator
 * @since 2019/04/28
 */
@Slf4j
public class MetaschemaImporter extends HeavyTask<String> {

    private JSONObject data;

    private Map<Field, JSONObject> picklistHolders = new HashMap<>();

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
        if (hasError != null) return hasError;

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
            return Language.L("实体名称已存在 : %s",entityName);
        }

        for (Object o : entity.getJSONArray("fields")) {
            JSONObject field = (JSONObject) o;
            String dt = field.getString("displayType");

            if (DisplayType.REFERENCE.name().equals(dt)
                    || DisplayType.N2NREFERENCE.name().equals(dt)) {
                String refEntity = field.getString("refEntity");
                if (!entityName.equals(refEntity) && !MetadataHelper.containsEntity(refEntity)) {
                    return Language.L("缺少必要的引用实体 : %s (%s)", field.getString("fieldLabel"), refEntity);
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

        final ID sessionUser = UserContextHolder.getUser(true);
        if (sessionUser == null) UserContextHolder.setUser(getUser());

        // 字段选项
        try {
            for (Map.Entry<Field, JSONObject> e : picklistHolders.entrySet()) {
                Field field = e.getKey();
                JSONObject config = e.getValue();

                JSONArray options = config.getJSONArray("show");
                for (Object o : options) {
                    String keepId = ((JSONObject) o).getString(KEEP_ID);
                    if (ID.isId(keepId)) {
                        Record c = EntityHelper.forNew(EntityHelper.PickList, UserService.SYSTEM_USER, true);
                        c.setString("belongEntity", field.getOwnEntity().getName());
                        c.setString("belongField", field.getName());
                        c.setString("text", "temp");

                        ((PersistManagerImpl) Application.getPersistManagerFactory().createPersistManager())
                                .saveInternal(c, ID.valueOf(keepId));
                        ((JSONObject) o).put("id", keepId);
                    }
                }

                Application.getBean(PickListService.class).updateBatch(field, config);
            }
        } catch (Exception ex) {
            log.warn("Importing PickList error : {}", ex.getLocalizedMessage());
        }

        setCompleted(100);

        if (sessionUser == null) UserContextHolder.clear();
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
     * @param schema
     * @param mainEntity
     * @return
     * @throws MetadataModificationException
     */
    private String performEntity(JSONObject schema, String mainEntity) throws MetadataModificationException {
        final String entityName = schema.getString("entity");
        final String entityLabel = schema.getString("entityLabel");

        Entity2Schema entity2Schema = new Entity2Schema(this.getUser());
        entity2Schema.createEntity(
                entityName, entityLabel, schema.getString("comments"), mainEntity, Boolean.FALSE, Boolean.FALSE);

        Entity newEntity = MetadataHelper.getEntity(entityName);
        this.setCompleted((int) (this.getCompleted() * 1.5));

        JSONArray fields = schema.getJSONArray("fields");
        try {
            List<Field> fieldsList = new ArrayList<>();
            Set<String> uniqueKeyFields = new HashSet<>();
            for (Object field : fields) {
                Field unsafe = performField((JSONObject) field, newEntity);
                if (unsafe != null) {
                    fieldsList.add(unsafe);

                    if (DisplayType.SERIES.name().equals(((JSONObject) field).getString("displayType"))) {
                        uniqueKeyFields.add(unsafe.getName());
                    }
                }
            }

            // 同步字段到数据库
            new Field2Schema(UserService.ADMIN_USER).schema2Database(
                    newEntity, fieldsList.toArray(new Field[0]), uniqueKeyFields);

        } catch (Exception ex) {
            entity2Schema.dropEntity(newEntity, true);

            if (ex instanceof MetadataModificationException) {
                throw ex;
            } else {
                throw new MetadataModificationException(ex);
            }
        }

        Record needUpdate = EntityHelper.forUpdate(
                EasyMetaFactory.valueOf(newEntity).getMetaId(), this.getUser(), false);

        String nameField = schema.getString("nameField");
        if (nameField != null) {
            needUpdate.setString("nameField", nameField);
        }
        String entityIcon = schema.getString("entityIcon");
        if (entityIcon != null) {
            needUpdate.setString("icon", entityIcon);
        }
        String quickFields = schema.getString("quickFields");
        if (quickFields != null) {
            needUpdate.setString("extConfig", JSONUtils.toJSONObject("quickFields", quickFields).toJSONString());
        }

        if (needUpdate.getAvailableFieldIterator().hasNext()) {
            Application.getCommonsService().update(needUpdate);
        }

        // 刷新元数据
        MetadataHelper.getMetadataFactory().refresh();

        // 表单回填
        JSONArray fillins = schema.getJSONArray(MetaSchemaGenerator.CFG_FILLINS);
        if (fillins != null) {
            for (Object o : fillins) {
                performFillin(entityName, (JSONObject) o);
            }
        }

        // 布局
        JSONObject layouts = schema.getJSONObject(MetaSchemaGenerator.CFG_LAYOUTS);
        if (layouts != null) {
            for (Map.Entry<String, Object> e : layouts.entrySet()) {
                performLayout(entityName, e.getKey(), (JSON) e.getValue());
            }
        }

        // 高级查询
        JSONArray filters = schema.getJSONArray(MetaSchemaGenerator.CFG_FILTERS);
        if (filters != null) {
            for (Object o : filters) {
                performFilter(entityName, (JSONObject) o);
            }
        }

        // 触发器
        JSONArray triggers = schema.getJSONArray(MetaSchemaGenerator.CFG_TRIGGERS);
        if (triggers != null) {
            for (Object o : triggers) {
                performTrigger(entityName, (JSONObject) o);
            }
        }

        // 审批流程
        JSONArray approvals = schema.getJSONArray(MetaSchemaGenerator.CFG_APPROVALS);
        if (approvals != null) {
            for (Object o : approvals) {
                performApproval(entityName, (JSONObject) o);
            }
        }

        // 记录转换
        JSONArray transforms = schema.getJSONArray(MetaSchemaGenerator.CFG_TRANSFORMS);
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
                belong,
                fieldName,
                fieldLabel,
                dt,
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
            picklistHolders.put(unsafeField, performPickList(schemaField.getJSONArray("items")));
        }

        return unsafeField;
    }

    private JSONObject performPickList(JSONArray items) {
        JSONArray shown = new JSONArray();
        for (Object o : items) {
            JSONArray item = o instanceof Object[] ? (JSONArray) JSON.toJSON(o) : (JSONArray) o;

            JSONObject option = JSONUtils.toJSONObject(
                    new String[] { "text", "default" },
                    new Object[] { item.getString(0), item.getBoolean(1) });
            // MultiSelect
            if (item.size() > 2) {
                option.put("mask", item.getLongValue(2));
            }

            // v2.10: Color, Id
            if (item.size() > 3) option.put("color", item.getString(3));
            if (item.size() > 4) option.put(KEEP_ID, item.getString(4));

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

    private void performTrigger(String entity, JSONObject config) {
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
        String keepId = config.getString(KEEP_ID);
        if (ID.isId(keepId)) {
            ((PersistManagerImpl) Application.getPersistManagerFactory().createPersistManager())
                    .saveInternal(record, ID.valueOf(keepId));
            record = EntityHelper.forUpdate(record.getPrimary(), UserService.SYSTEM_USER, false);

            Application.getBean(TransformConfigService.class).update(record);
        } else {
            Application.getBean(TransformConfigService.class).create(record);
        }
    }
}
