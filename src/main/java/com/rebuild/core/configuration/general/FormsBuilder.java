/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
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
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.state.StateManager;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
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

    protected FormsBuilder() {
    }

    // 分割线
    public static final String DIVIDER_LINE = "$DIVIDER$";

    // 引用主记录
    public static final String DV_MAINID = "$MAINID$";

    // 引用记录
    public static final String DV_REFERENCE_PREFIX = "&";

    // 自动只读
    private static final int READONLYW_RO = 2;
    // 自动只读-表单回填
    private static final int READONLYW_RW = 3;

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

        // 明细实体有主实体
        final Entity hasMainEntity = entityMeta.getMainEntity();
        // 审批流程（状态）
        ApprovalState approvalState;
        String readonlyMessage = null;

        // 判断表单权限

        // 新建
        if (record == null) {
            if (hasMainEntity != null) {
                ID mainid = FormsBuilderContextHolder.getMainIdOfDetail(false);
                Assert.notNull(mainid, "Call `FormBuilderContextHolder#setMainIdOfDetail` first!");

                approvalState = EntityHelper.isUnsavedId(mainid) ? null : getHadApproval(hasMainEntity, mainid);
                if ((approvalState == ApprovalState.PROCESSING || approvalState == ApprovalState.APPROVED)) {
                    readonlyMessage = approvalState == ApprovalState.APPROVED
                            ? Language.L("主记录已完成审批，不能添加明细")
                            : Language.L("主记录正在审批中，不能添加明细");
                }
                // 明细无需审批
                approvalState = null;

                if (!EntityHelper.isUnsavedId(mainid)
                        && !Application.getPrivilegesManager().allowUpdate(user, mainid)) {
                    return formatModelError(Language.L("你没有添加明细权限"));
                }

            } else if (!Application.getPrivilegesManager().allowCreate(user, entityMeta.getEntityCode())) {
                return formatModelError(Language.L("你没有新建权限" ));
            } else {
                approvalState = getHadApproval(entityMeta, null);
            }
        }
        // 查看（视图）
        else if (viewMode) {
            if (!Application.getPrivilegesManager().allowRead(user, record)) {
                return formatModelError(Language.L("无权读取此记录或记录已被删除"));
            }

            approvalState = getHadApproval(entityMeta, record);

        }
        // 编辑
        else {
            if (!Application.getPrivilegesManager().allowUpdate(user, record)) {
                return formatModelError(Language.L("你没有修改此记录的权限"));
            }

            approvalState = getHadApproval(entityMeta, record);
            if (approvalState != null) {
                String recordType = hasMainEntity == null ? Language.L("记录") : Language.L("主记录");
                if (approvalState == ApprovalState.APPROVED) {
                    readonlyMessage = Language.L("%s已完成审批，禁止编辑", recordType);
                } else if (approvalState == ApprovalState.PROCESSING) {
                    readonlyMessage = Language.L("%s正在审批中，禁止编辑", recordType);
                }
            }
        }

        ConfigBean model = getFormLayout(entity, user);
        JSONArray elements = (JSONArray) model.getJSON("elements");
        if (elements == null || elements.isEmpty()) {
            return formatModelError(Language.L("此表单布局尚未配置，请配置后使用"));
        }

        Record recordData = null;
        if (record != null) {
            recordData = findRecord(record, user, elements);
            if (recordData == null) {
                return formatModelError(Language.L("无权读取此记录或记录已被删除"));
            }
        }

        // 自动只读
        Set<String> roAutos = EasyMetaFactory.getAutoReadonlyFields(entity);
        Set<String> roAutosWithout = record == null ? null : Collections.emptySet();
        for (Object o : elements) {
            JSONObject field = (JSONObject) o;
            if (roAutos.contains(field.getString("field")) || readonlyMessage != null) {
                field.put("readonly", true);

                // 前端可收集值
                if (roAutosWithout == null) roAutosWithout = AutoFillinManager.instance.getAutoReadonlyFields(entity);
                if (roAutosWithout.contains(field.getString("field"))) {
                    field.put("readonlyw", READONLYW_RW);
                } else {
                    field.put("readonlyw", READONLYW_RO);
                }
            }
        }

        buildModelElements(elements, entityMeta, recordData, user, !viewMode);

        if (elements.isEmpty()) {
            return formatModelError(Language.L("此表单布局尚未配置，请配置后使用"));
        }

        // 主/明细实体处理
        if (hasMainEntity != null) {
            model.set("mainMeta", EasyMetaFactory.toJSON(hasMainEntity));
        } else if (entityMeta.getDetailEntity() != null) {
            // v3.1
            if (!entityMeta.getExtraAttrs().getBooleanValue(EasyEntityConfigProps.NOT_COEDITING)) {
                model.set("detailMeta", EasyMetaFactory.toJSON(entityMeta.getDetailEntity()));
                model.set("detailsNotEmpty", entityMeta.getExtraAttrs().getBooleanValue(EasyEntityConfigProps.DETAILS_NOTEMPTY));
            }
        }

        if (recordData != null && recordData.hasValue(EntityHelper.ModifiedOn)) {
            model.set("lastModified", recordData.getDate(EntityHelper.ModifiedOn).getTime());
        }

        if (approvalState != null) model.set("hadApproval", approvalState.getState());
        if (readonlyMessage != null) model.set("readonlyMessage", readonlyMessage);

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
        // 新建时
        if (recordId == null) {
            return RobotApprovalManager.instance.hadApproval(entity, null);
        }

        // 普通实体
        if (entity.getMainEntity() == null) {
            return RobotApprovalManager.instance.hadApproval(entity, recordId);
        }

        // 明细实体

        ID mainid = FormsBuilderContextHolder.getMainIdOfDetail(false);
        if (mainid == null) {
            Field dtmField = MetadataHelper.getDetailToMainField(entity);
            Object[] o = Application.getQueryFactory().uniqueNoFilter(recordId, dtmField.getName());
            if (o == null) {
                log.warn("No main-id found : {}", recordId);
                return null;
            }

            mainid = (ID) o[0];
        }

        return RobotApprovalManager.instance.hadApproval(entity.getMainEntity(), mainid);
    }

    /**
     * 构建表单元素
     *
     * @param elements
     * @param entity
     * @param recordData
     * @param user
     * @param useAdvControl
     */
    protected void buildModelElements(JSONArray elements, Entity entity, Record recordData, ID user, boolean useAdvControl) {
        final User formUser = Application.getUserStore().getUser(user);
        final Date now = CalendarUtils.now();

        // 新建
        final boolean isNew = recordData == null || recordData.getPrimary() == null
                || EntityHelper.isUnsavedId(recordData.getPrimary());

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

            // v2.2 高级控制
            Object displayOnCreate = el.remove("displayOnCreate");
            Object displayOnUpdate = el.remove("displayOnUpdate");
            Object requiredOnCreate = el.remove("requiredOnCreate");
            Object requiredOnUpdate = el.remove("requiredOnUpdate");
            if (useAdvControl) {
                // 显示
                if (displayOnCreate != null && !(Boolean) displayOnCreate && isNew) {
                    iter.remove();
                    continue;
                }
                if (displayOnUpdate != null && !(Boolean) displayOnUpdate && !isNew) {
                    iter.remove();
                    continue;
                }

                // 必填
                if (requiredOnCreate != null && (Boolean) requiredOnCreate && isNew) {
                    el.put("nullable", false);
                }
                if (requiredOnUpdate != null && (Boolean) requiredOnUpdate && !isNew) {
                    el.put("nullable", false);
                }
            }

            // 自动只读的
            final boolean roViaAuto = el.getBooleanValue("readonly");

            final Field fieldMeta = entity.getField(fieldName);
            final EasyField easyField = EasyMetaFactory.valueOf(fieldMeta);
            final DisplayType dt = easyField.getDisplayType();
            el.put("label", easyField.getLabel());
            el.put("type", dt.name());

            el.put("readonly", (!isNew && !fieldMeta.isUpdatable()) || roViaAuto);

            // 优先使用指定值
            final Boolean nullable = el.getBoolean("nullable");
            if (nullable != null) {
                el.put("nullable", nullable);
            } else {
                el.put("nullable", fieldMeta.isNullable());
            }

            // 字段扩展配置 FieldExtConfigProps
            JSONObject fieldExtAttrs = easyField.getExtraAttrs(true);
            el.putAll(fieldExtAttrs);

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
            } else if (dt == DisplayType.TAG) {
                el.put("options", ObjectUtils.defaultIfNull(el.remove("tagList"), JSONUtils.EMPTY_ARRAY));
            } else if (dt == DisplayType.DATETIME) {
                String format = StringUtils.defaultIfBlank(
                        easyField.getExtraAttr(EasyFieldConfigProps.DATETIME_FORMAT),
                        easyField.getDisplayType().getDefaultFormat());
                el.put(EasyFieldConfigProps.DATETIME_FORMAT, format);
            } else if (dt == DisplayType.DATE) {
                String format = StringUtils.defaultIfBlank(
                        easyField.getExtraAttr(EasyFieldConfigProps.DATE_FORMAT),
                        easyField.getDisplayType().getDefaultFormat());
                el.put(EasyFieldConfigProps.DATE_FORMAT, format);
            } else if (dt == DisplayType.TIME) {
                String format = StringUtils.defaultIfBlank(
                        easyField.getExtraAttr(EasyFieldConfigProps.TIME_FORMAT),
                        easyField.getDisplayType().getDefaultFormat());
                el.put(EasyFieldConfigProps.TIME_FORMAT, format);
            } else if (dt == DisplayType.CLASSIFICATION) {
                el.put("openLevel", ClassificationManager.instance.getOpenLevel(fieldMeta));
            } else if (dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE) {
                Entity refEntity = fieldMeta.getReferenceEntity();
                boolean quickNew = el.getBooleanValue(EasyFieldConfigProps.REFERENCE_QUICKNEW);
                if (quickNew) {
                    el.put(EasyFieldConfigProps.REFERENCE_QUICKNEW,
                            Application.getPrivilegesManager().allowCreate(user, refEntity.getEntityCode()));
                    el.put("referenceEntity", EasyMetaFactory.toJSON(refEntity));
                }
            }

            // 编辑/视图
            if (recordData != null) {
                Object value = wrapFieldValue(recordData, easyField, user);
                if (value != null) {
                    el.put("value", value);
                }

                // 父级级联
                if ((dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE) && recordData.getPrimary() != null) {
                    ID parentValue = getCascadingFieldParentValue(easyField, recordData.getPrimary(), false);
                    if (parentValue != null) {
                        el.put("_cascadingFieldParentValue", parentValue);
                    }
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
                            el.put("value", FieldValueHelper.wrapMixValue(null, Language.L("未提交")));
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
                        el.put("readonlyw", READONLYW_RO);
                    } else {
                        Object defaultValue = easyField.exprDefaultValue();
                        if (defaultValue != null) {
                            el.put("value", easyField.wrapValue(defaultValue));
                        }
                    }
                }

                // 自动值
                if (roViaAuto && el.get("value") == null) {
                    if (dt == DisplayType.EMAIL
                            || dt == DisplayType.PHONE
                            || dt == DisplayType.URL
                            || dt == DisplayType.DATE
                            || dt == DisplayType.DATETIME
                            || dt == DisplayType.NUMBER
                            || dt == DisplayType.DECIMAL
                            || dt == DisplayType.SERIES
                            || dt == DisplayType.TEXT
                            || dt == DisplayType.NTEXT) {
                        Integer s = el.getInteger("readonlyw");
                        if (s == null) el.put("readonlyw", READONLYW_RO);
                    }
                }

                // v3.1 父级级联
                if (entity.getMainEntity() != null && (dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE)) {
                    ID mainid = FormsBuilderContextHolder.getMainIdOfDetail(false);
                    ID parentValue = EntityHelper.isUnsavedId(mainid) ? null
                            : getCascadingFieldParentValue(easyField, mainid, true);
                    if (parentValue != null) {
                        el.put("_cascadingFieldParentValue", parentValue);
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
    protected Record findRecord(ID id, ID user, JSONArray elements) {
        if (elements.isEmpty()) return null;

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
        return Application.createQuery(sql.toString(), user).setParameter(1, id).record();
    }

    /**
     * 封装表单/布局所用的字段值
     *
     * @param data
     * @param field
     * @param user4Desensitized 不传则不脱敏
     * @return
     * @see FieldValueHelper#wrapFieldValue(Object, EasyField)
     * @see com.rebuild.core.support.general.DataListWrapper#wrapFieldValue(Object, Field)
     */
    public Object wrapFieldValue(Record data, EasyField field, ID user4Desensitized) {
        final DisplayType dt = field.getDisplayType();
        Object value = data.getObjectValue(field.getName());

        // 使用主键
        if (dt == DisplayType.BARCODE) {
            value = data.getPrimary();
        }

        // 处理日期格式
        if (dt == DisplayType.REFERENCE
                && value instanceof ID && ((ID) value).getLabelRaw() != null) {
            Field nameField = field.getRawMeta().getReferenceEntity().getNameField();

            if (nameField.getType() == FieldType.DATE || nameField.getType() == FieldType.TIMESTAMP) {
                Object rawLabel = ((ID) value).getLabelRaw();
                try {
                    Object newLabel = EasyMetaFactory.valueOf(nameField).wrapValue(rawLabel);
                    ((ID) value).setLabel(newLabel);
                } catch (IllegalArgumentException ex) {
                    log.warn("Field [{}] format error : {}", nameField, ex.getLocalizedMessage());
                }
            }
        }

        value = FieldValueHelper.wrapFieldValue(value, field);

        if (value != null) {
            if (FieldValueHelper.isUseDesensitized(field, user4Desensitized)) {
                value = FieldValueHelper.desensitized(field, value);
            }
            // v3.1.4
            else if (dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE) {

                Field nameField = field.getRawMeta().getReferenceEntity().getNameField();
                EasyField easyNameField = EasyMetaFactory.valueOf(nameField);

                if (FieldValueHelper.isUseDesensitized(easyNameField, user4Desensitized)) {
                    FieldValueHelper.desensitizedMixValue(easyNameField, (JSON) value);
                }
            }
        }
        return value;
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

        // 保持在初始值中
        // TODO 更多保持字段
        Set<String> initialValKeeps = new HashSet<>();

        Map<String, Object> initialValReady = new HashMap<>();
        for (Map.Entry<String, Object> e : initialVal.entrySet()) {
            final String field = e.getKey();
            final String value = (String) e.getValue();
            if (StringUtils.isBlank(value)) continue;

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
                Object mixValue = inFormFields.contains(dtmField.getName())
                        ? getReferenceMixValue(value)
                        : (isNewMainId(value) ? EntityHelper.UNSAVED_ID : value);

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
        if (isNewMainId(idValue)) {
            return FieldValueHelper.wrapMixValue(EntityHelper.UNSAVED_ID, Language.L("新的"));
        } else if (!ID.isId(idValue)) {
            return null;
        }

        try {
            String idLabel = FieldValueHelper.getLabel(ID.valueOf(idValue));
            return FieldValueHelper.wrapMixValue(ID.valueOf(idValue), idLabel);
        } catch (NoRecordFoundException ex) {
            log.error("No record found : " + idValue);
            return null;
        }
    }

    /**
     * 父级（值）级联
     *
     * @param field
     * @param record
     * @param recordIsMain
     * @return
     */
    private ID getCascadingFieldParentValue(EasyField field, ID record, boolean recordIsMain) {
        String pf = field.getExtraAttr("_cascadingFieldParent");
        if (pf == null) return null;

        String[] pfs = pf.split(MetadataHelper.SPLITER_RE);
        String fieldParent = pfs[0];

        // 明细字段使用主实体字段
        // format: MAINENTITY.FIELD
        boolean useMainField = pfs[0].contains(".");

        if (recordIsMain) {
            if (useMainField) {
                fieldParent = pfs[0].split("\\.")[1];
            } else {
                return null;
            }
        } else if (useMainField) {
            Field dtf = MetadataHelper.getDetailToMainField(field.getRawMeta().getOwnEntity());
            fieldParent = dtf.getName() + "." + pfs[0].split("\\.")[1];
        }

        // v3.1.1 父级已删除
        Entity entity = MetadataHelper.getEntity(record.getEntityCode());
        if (MetadataHelper.getLastJoinField(entity, fieldParent) == null) {
            log.warn("Unknow field : {} in {}", fieldParent, entity.getName());
            return null;
        }

        Object[] o = Application.getQueryFactory().uniqueNoFilter(record, fieldParent);
        return o == null ? null : (ID) o[0];
    }

    private boolean isNewMainId(Object id) {
        return DV_MAINID.equals(id) || EntityHelper.isUnsavedId(id);
    }
}
