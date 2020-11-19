/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.state.StateManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.*;

/**
 * 表单构造
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/03
 */
@Slf4j
public class FormsBuilder extends FormsManager {

    public static final FormsBuilder instance = new FormsBuilder();

    private FormsBuilder() {
    }

    // 分割线
    public static final String DIVIDER_LINE = "$DIVIDER$";
    // 引用主记录
    public static final String DV_MAINID = "$MAINID$";
    // 引用记录
    public static final String DV_REFERENCE_PREFIX = "&";

    /**
     * 表单-编辑
     *
     * @param entity
     * @param user
     * @param record null 表示新建
     * @return
     */
    public JSON buildForm(String entity, ID user, ID record) {
        return buildModel(entity, user, record, false);
    }

    /**
     * 视图
     *
     * @param entity
     * @param user
     * @param record
     * @return
     */
    public JSON buildView(String entity, ID user, ID record) {
        Assert.notNull(record, "[record] cannot be null");
        return buildModel(entity, user, record, true);
    }

    /**
     * @param entity
     * @param user
     * @param record
     * @param viewMode 视图模式
     * @return
     */
    private JSON buildModel(String entity, ID user, ID record, boolean viewMode) {
        Assert.notNull(entity, "[entity] cannot be null");
        Assert.notNull(user, "[user] cannot be null");

        final Entity entityMeta = MetadataHelper.getEntity(entity);
        if (record != null) {
            Assert.isTrue(entityMeta.getEntityCode().equals(record.getEntityCode()), "[entity] and [record] do not match");
        }

        // 明细实体
        final Entity mainEntity = entityMeta.getMainEntity();
        // 审批流程（状态）
        ApprovalState approvalState;

        // 判断表单权限

        // 新建
        if (record == null) {
            if (mainEntity != null) {
                ID mainId = FormBuilderContextHolder.getMainIdOfDetail();
                Assert.notNull(mainId, "Please calls FormBuilderContextHolder#setMainIdOfDetail first");

                approvalState = getHadApproval(entityMeta, null);

                if ((approvalState == ApprovalState.PROCESSING || approvalState == ApprovalState.APPROVED)) {
                    String stateType = approvalState == ApprovalState.APPROVED ? "RecordApproved" : "RecordInApproval";
                    return formatModelError(Language.L("MainRecordApprovedNotAddDetailTips", stateType));
                }

                // 明细无需审批
                approvalState = null;

                if (!Application.getPrivilegesManager().allowUpdate(user, mainId)) {
                    return formatModelError(Language.LF("YouNoSomePermission", "AddDetail"));
                }
            } else if (!Application.getPrivilegesManager().allowCreate(user, entityMeta.getEntityCode())) {
                return formatModelError(Language.L("YouNoSomePermission", "Create"));
            } else {
                approvalState = getHadApproval(entityMeta, null);
            }
        }
        // 查看（视图）
        else if (viewMode) {
            if (!Application.getPrivilegesManager().allowRead(user, record)) {
                return formatModelError(Language.L("NotReadRecordTips"));
            }

            approvalState = getHadApproval(entityMeta, record);

        }
        // 编辑
        else {
            if (!Application.getPrivilegesManager().allowUpdate(user, record)) {
                return formatModelError(Language.L("YouNoSomeRecordPermission", "Update"));
            }

            approvalState = getHadApproval(entityMeta, record);
            if (approvalState != null) {
                String recordType = mainEntity == null ? "Record" : "MainRecord";
                if (approvalState == ApprovalState.APPROVED) {
                    return formatModelError(Language.L("SomeRecordApprovedTips", recordType));
                } else if (approvalState == ApprovalState.PROCESSING) {
                    return formatModelError(Language.L("SomeRecordInApprovalTips", recordType));
                }
            }
        }

        ConfigBean model = getFormLayout(entity, user);
        JSONArray elements = (JSONArray) model.getJSON("elements");
        if (elements == null || elements.isEmpty()) {
            return formatModelError(Language.L("FormUnLayoutTips"));
        }

        Record data = null;
        if (record != null) {
            data = findRecord(record, user, elements);
            if (data == null) {
                return formatModelError(Language.L("NotReadRecordTips"));
            }
        }

        // 自动只读
        Set<String> roAutos = EasyMetaFactory.getAutoReadonlyFields(entity);
        for (Object o : elements) {
            JSONObject field = (JSONObject) o;
            if (roAutos.contains(field.getString("field"))) {
                field.put("readonly", true);
            }
        }

        buildModelElements(elements, entityMeta, data, user);

        if (elements.isEmpty()) {
            return formatModelError(Language.L("FormUnLayoutTips"));
        }

        // 主/明细实体处理
        if (entityMeta.getMainEntity() != null) {
            model.set("isDetail", true);
            model.set("mainMeta", EasyMetaFactory.toJSON(entityMeta.getMainEntity()));
        } else if (entityMeta.getDetailEntity() != null) {
            model.set("isMain", true);
            model.set("detailMeta", EasyMetaFactory.toJSON(entityMeta.getDetailEntity()));
        }

        if (data != null && data.hasValue(EntityHelper.ModifiedOn)) {
            model.set("lastModified", data.getDate(EntityHelper.ModifiedOn).getTime());
        }

        if (approvalState != null) {
            model.set("hadApproval", approvalState.getState());
        }

        model.set("id", null);  // Clean form's ID of config
        return model.toJSON();
    }

    /**
     * @param error
     * @return
     */
    private JSONObject formatModelError(String error) {
        JSONObject cfg = new JSONObject();
        cfg.put("error", error);
        return cfg;
    }

    /**
     * @param entity
     * @param recordId
     * @return
     * @see RobotApprovalManager#hadApproval(Entity, ID)
     */
    private ApprovalState getHadApproval(Entity entity, ID recordId) {
        Entity mainEntity = entity.getMainEntity();
        if (mainEntity == null) {
            return RobotApprovalManager.instance.hadApproval(entity, recordId);
        }

        ID mainId = FormBuilderContextHolder.getMainIdOfDetail();
        if (mainId == null) {
            Field dtmField = MetadataHelper.getDetailToMainField(entity);
            String sql = String.format("select %s from %s where %s = ?",
                    dtmField.getName(), entity.getName(), entity.getPrimaryField().getName());
            Object[] o = Application.createQueryNoFilter(sql).setParameter(1, recordId).unique();
            if (o == null) {
                return null;
            }
            mainId = (ID) o[0];
        }
        return RobotApprovalManager.instance.hadApproval(mainEntity, mainId);
    }

    /**
     * 构建表单元素
     *
     * @param elements
     * @param entity
     * @param data
     * @param user
     */
    public void buildModelElements(JSONArray elements, Entity entity, Record data, ID user) {
        final User formUser = Application.getUserStore().getUser(user);
        final Date now = CalendarUtils.now();
        final boolean hideUncreate = RebuildConfiguration.getBool(ConfigurationItem.FormHideUncreateField) && data == null;

        final String autoValue = Language.L("AutoValue");

        // Check and clean
        for (Iterator<Object> iter = elements.iterator(); iter.hasNext(); ) {
            JSONObject el = (JSONObject) iter.next();
            String fieldName = el.getString("field");
            if (DIVIDER_LINE.equalsIgnoreCase(fieldName)) {
                continue;
            }
            // 已删除字段
            if (!MetadataHelper.checkAndWarnField(entity, fieldName)) {
                iter.remove();
                continue;
            }

            // 自动只读
            final boolean roViaAuto = el.getBooleanValue("readonly");

            final Field fieldMeta = entity.getField(fieldName);
            if (hideUncreate && (!fieldMeta.isCreatable() || roViaAuto)) {
                iter.remove();
                continue;
            }

            final EasyField easyField = EasyMetaFactory.valueOf(fieldMeta);
            final DisplayType dt = easyField.getDisplayType();
            el.put("label", easyField.getLabel());
            el.put("type", dt.name());
            // 不可更新字段
            el.put("readonly", (data != null && !fieldMeta.isUpdatable()) || roViaAuto);

            // 优先使用指定值
            final Boolean nullable = el.getBoolean("nullable");
            if (nullable != null) {
                el.put("nullable", nullable);
            } else {
                el.put("nullable", fieldMeta.isNullable());
            }

            // 字段扩展配置 FieldExtConfigProps
            JSONObject fieldExt = easyField.getExtraAttrs(true);
            for (Map.Entry<String, Object> e : fieldExt.entrySet()) {
                el.put(e.getKey(), e.getValue());
            }

            // 不同字段类型的处理

            if (dt == DisplayType.PICKLIST) {
                JSONArray options = PickListManager.instance.getPickList(fieldMeta);
                el.put("options", options);
            } else if (dt == DisplayType.STATE) {
                JSONArray options = StateManager.instance.getStateOptions(fieldMeta);
                el.put("options", options);
                el.remove(EasyFieldConfigProps.STATE_CLASS);
            } else if (dt == DisplayType.MULTISELECT) {
                JSONArray options = MultiSelectManager.instance.getSelectList(fieldMeta);
                el.put("options", options);
            } else if (dt == DisplayType.DATETIME) {
                el.put(EasyFieldConfigProps.DATETIME_FORMAT,
                        easyField.getExtraAttr(EasyFieldConfigProps.DATETIME_FORMAT));
            } else if (dt == DisplayType.DATE) {
                el.put(EasyFieldConfigProps.DATE_FORMAT,
                        easyField.getExtraAttr(EasyFieldConfigProps.DATE_FORMAT));
            } else if (dt == DisplayType.CLASSIFICATION) {
                el.put("openLevel", ClassificationManager.instance.getOpenLevel(fieldMeta));
            }

            // 编辑/视图
            if (data != null) {
                Object value = wrapFieldValue(data, easyField);
                if (value != null) {
                    el.put("value", value);
                }
            }
            // 新建记录
            else {
                if (!fieldMeta.isCreatable()) {
                    el.put("readonly", true);
                    switch (fieldName) {
                        case EntityHelper.CreatedOn:
                        case EntityHelper.ModifiedOn:
                            el.put("value", CalendarUtils.getUTCDateTimeFormat().format(now));
                            break;
                        case EntityHelper.CreatedBy:
                        case EntityHelper.ModifiedBy:
                        case EntityHelper.OwningUser:
                            el.put("value", FieldValueHelper.wrapMixValue(formUser.getId(), formUser.getFullName()));
                            break;
                        case EntityHelper.OwningDept:
                            Department dept = formUser.getOwningDept();
                            Assert.notNull(dept, "Department of user is unset : " + formUser.getId());
                            el.put("value", FieldValueHelper.wrapMixValue((ID) dept.getIdentity(), dept.getName()));
                            break;
                        case EntityHelper.ApprovalId:
                            el.put("value", FieldValueHelper.wrapMixValue(null, Language.L("UnSubmit")));
                            break;
                        case EntityHelper.ApprovalState:
                            el.put("value", ApprovalState.DRAFT.getState());
                            break;
                        default:
                            break;
                    }
                }

                // 默认值
                if (el.get("value") == null) {
                    if (dt == DisplayType.SERIES) {
                        el.put("value", autoValue);
                    } else {
                        Object defaultValue = easyField.exprDefaultValue();
                        if (defaultValue != null) {
                            el.put("value", easyField.wrapValue(defaultValue));
                        }
                    }
                }

                // 触发器自动值
                if (roViaAuto && el.get("value") == null) {
                    if (dt == DisplayType.REFERENCE || dt == DisplayType.CLASSIFICATION) {
                        el.put("value", FieldValueHelper.wrapMixValue(null, autoValue));

                    } else if (dt == DisplayType.N2NREFERENCE) {
                        JSONArray arrayValue = new JSONArray();
                        arrayValue.add(FieldValueHelper.wrapMixValue(null, autoValue));
                        el.put("value", arrayValue);

                    } else if (dt == DisplayType.EMAIL
                            || dt == DisplayType.PHONE
                            || dt == DisplayType.URL
                            || dt == DisplayType.DATE
                            || dt == DisplayType.DATETIME
                            || dt == DisplayType.NUMBER
                            || dt == DisplayType.DECIMAL
                            || dt == DisplayType.SERIES
                            || dt == DisplayType.TEXT
                            || dt == DisplayType.NTEXT) {
                        el.put("value", autoValue);
                    }
                }

            }  // end 新建记录
        }  // end for
    }

    /**
     * @param id
     * @param user
     * @param elements
     * @return
     */
    public Record findRecord(ID id, ID user, JSONArray elements) {
        if (elements.isEmpty()) {
            return null;
        }

        Entity entity = MetadataHelper.getEntity(id.getEntityCode());
        StringBuilder sql = new StringBuilder("select ");
        for (Object element : elements) {
            JSONObject el = (JSONObject) element;
            String field = el.getString("field");
            if (field.startsWith("$") || !entity.containsField(field)) {
                continue;
            }

            // REFERENCE
            if (EasyMetaFactory.getDisplayType(entity.getField(field)) == DisplayType.REFERENCE) {
                sql.append('&').append(field).append(',');
            }
            sql.append(field).append(',');
        }

        // Append fields
        sql.append(entity.getPrimaryField().getName());
        if (entity.containsField(EntityHelper.ModifiedOn)) {
            sql.append(',').append(EntityHelper.ModifiedOn);
        }

        sql.append(" from ")
                .append(entity.getName())
                .append(" where ")
                .append(entity.getPrimaryField().getName())
                .append(" = ?");
        return Application.getQueryFactory().createQuery(sql.toString(), user).setParameter(1, id).record();
    }

    /**
     * 封装表单/布局所用的字段值
     *
     * @param data
     * @param field
     * @return
     * @see EasyField#wrapValue(Object)
     * @see FieldValueHelper#wrapFieldValue(Object, EasyField)
     */
    public Object wrapFieldValue(Record data, EasyField field) {
        return FieldValueHelper.wrapFieldValue(data.getObjectValue(field.getName()), field);
    }

    /**
     * 表单初始值填充
     *
     * @param entity
     * @param formModel
     * @param initialVal 此值优先级大于字段默认值
     */
    public void setFormInitialValue(Entity entity, JSON formModel, JSONObject initialVal) {
        if (initialVal == null || initialVal.isEmpty()) {
            return;
        }

        JSONArray elements = ((JSONObject) formModel).getJSONArray("elements");
        if (elements == null || elements.isEmpty()) {
            return;
        }

        // 已布局字段。字段是否布局会影响返回值
        Set<String> inFormFields = new HashSet<>();
        for (Object o : elements) {
            inFormFields.add(((JSONObject) o).getString("field"));
        }

        // 保持在初始值中（TODO 更多保持字段）
        Set<String> initialValKeeps = new HashSet<>();

        Map<String, Object> initialValReady = new HashMap<>();
        for (Map.Entry<String, Object> e : initialVal.entrySet()) {
            final String field = e.getKey();
            final String value = (String) e.getValue();
            if (StringUtils.isBlank(value)) {
                continue;
            }

            // 引用字段值如 `&User`
            if (field.startsWith(DV_REFERENCE_PREFIX)) {
                Object mixValue = getReferenceMixValue(value);
                if (mixValue != null) {
                    Entity source = MetadataHelper.getEntity(field.substring(1));
                    Field[] reftoFields = MetadataHelper.getReferenceToFields(source, entity);
                    // 如有多个则全部填充
                    for (Field refto : reftoFields) {
                        initialValReady.put(refto.getName(), inFormFields.contains(refto.getName()) ? mixValue : value);
                    }
                }

            }
            // 主实体字段
            else if (field.equals(DV_MAINID)) {
                Field dtmField = MetadataHelper.getDetailToMainField(entity);
                Object mixValue = inFormFields.contains(dtmField.getName()) ? getReferenceMixValue(value) : value;
                if (mixValue != null) {
                    initialValReady.put(dtmField.getName(), mixValue);
                    initialValKeeps.add(dtmField.getName());
                }

            }
            // 其他
            else if (entity.containsField(field)) {
                if (EasyMetaFactory.getDisplayType(entity.getField(field)) == DisplayType.REFERENCE) {
                    Object mixValue = inFormFields.contains(field) ? getReferenceMixValue(value) : value;
                    if (mixValue != null) {
                        initialValReady.put(field, mixValue);
                    }
                }

            } else {
                log.warn("Unknown value pair : " + field + " = " + value);
            }
        }

        if (initialValReady.isEmpty()) return;

        // 已布局的移除
        for (Object o : elements) {
            JSONObject item = (JSONObject) o;
            String field = item.getString("field");
            if (initialValReady.containsKey(field)) {
                item.put("value",
                        initialValKeeps.contains(field) ? initialValReady.get(field) : initialValReady.remove(field));
            }
        }

        // 没布局出来的也需要返回（放入 initialValue 节点）
        // 如明细记录中的主实体字段值
        if (!initialValReady.isEmpty()) {
            ((JSONObject) formModel).put("initialValue", initialValReady);
        }
    }

    /**
     * 引用字段值
     *
     * @param idValue
     * @return returns [ID, LABEL]
     */
    private JSON getReferenceMixValue(String idValue) {
        if (!ID.isId(idValue)) return null;

        try {
            String idLabel = FieldValueHelper.getLabel(ID.valueOf(idValue));
            return FieldValueHelper.wrapMixValue(ID.valueOf(idValue), idLabel);
        } catch (NoRecordFoundException ex) {
            log.error("No record found : " + idValue);
            return null;
        }
    }
}
