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
import com.rebuild.core.metadata.EntityRecordCreator;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyDecimal;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.privileges.FieldPrivileges;
import com.rebuild.core.privileges.UserFilters;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.core.service.general.GeneralEntityService;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.License;
import com.rebuild.core.support.general.DataListWrapper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.state.StateManager;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
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
    // 引用
    public static final String REFFORM_LINE = "$REFFORM$";

    // 引用主记录
    public static final String DV_MAINID = "$MAINID$";
    public static final String DV_MAINID_FJS = "$MAINID$FJS";

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
     * @param recordId null 表示新建
     * @return
     */
    public JSON buildForm(String entity, ID user, ID recordId) {
        return buildModel(entity, user, recordId, false);
    }

    /**
     * 视图
     *
     * @param entity
     * @param user
     * @param recordId
     * @return
     */
    public JSON buildView(String entity, ID user, ID recordId) {
        Assert.notNull(recordId, "[recordId] cannot be null");
        return buildModel(entity, user, recordId, true);
    }

    /**
     * @param entity
     * @param user
     * @param recordId
     * @param viewMode 视图模式
     * @return
     */
    private JSON buildModel(String entity, ID user, ID recordId, boolean viewMode) {
        Assert.notNull(entity, "[entity] cannot be null");
        Assert.notNull(user, "[user] cannot be null");

        final Entity entityMeta = MetadataHelper.getEntity(entity);
        if (recordId != null) {
            Assert.isTrue(entityMeta.getEntityCode().equals(recordId.getEntityCode()), "[entity] and [recordId] do not matchs");

            if (MetadataHelper.isBizzEntity(entityMeta) && !UserFilters.allowAccessBizz(user, recordId)) {
                return formatModelError(Language.L("无权读取此记录或记录已被删除"));
            }
        }

        // 明细实体
        final Entity hasMainEntity = entityMeta.getMainEntity();
        // 审批流程（状态）
        ApprovalState approvalState;
        // 提示
        String readonlyMessage = null;

        // 判断表单权限

        // 新建
        if (recordId == null) {
            if (hasMainEntity != null) {
                ID mainid = FormsBuilderContextHolder.getMainIdOfDetail(false);
                Assert.notNull(mainid, "CALL `FormBuilderContextHolder#setMainIdOfDetail` FIRST!");

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
            if (!Application.getPrivilegesManager().allowRead(user, recordId)) {
                return formatModelError(Language.L("无权读取此记录或记录已被删除"));
            }

            approvalState = getHadApproval(entityMeta, recordId);

        }
        // 编辑
        else {
            if (!Application.getPrivilegesManager().allowUpdate(user, recordId)) {
                return formatModelError(Language.L("你没有编辑此记录的权限"));
            }

            approvalState = getHadApproval(entityMeta, recordId);
            if (approvalState != null) {
                String recordType = hasMainEntity == null ? Language.L("记录") : Language.L("主记录");
                if (approvalState == ApprovalState.APPROVED) {
                    readonlyMessage = Language.L("%s已完成审批，禁止编辑", recordType);
                } else if (approvalState == ApprovalState.PROCESSING) {
                    readonlyMessage = Language.L("%s正在审批中，禁止编辑", recordType);
                }
            }
        }

        // v3.7 指定布局
        ID recordOrLayoutId = recordId;

        // 优先使用
        ID forceLayout = FormsBuilderContextHolder.getSpecLayout(false);
        if (forceLayout != null) recordOrLayoutId = forceLayout;
        // 明细共同编辑
        if (forceLayout == null && entityMeta.getMainEntity() != null && recordId == null) {
            ID mainid = FormsBuilderContextHolder.getMainIdOfDetail(false);
            if (mainid != null && !EntityHelper.isUnsavedId(mainid)) {
                List<ID> ids = QueryHelper.detailIdsNoFilter(mainid, entityMeta);
                if (!ids.isEmpty()) recordOrLayoutId = ids.get(0);
            }
        }

        int applyType = recordId == null ? FormsManager.APPLY_NEW : FormsManager.APPLY_EDIT;
        if (viewMode) applyType = FormsManager.APPLY_VIEW;

        ConfigBean model = getFormLayout(entity, recordOrLayoutId, applyType);
        JSONArray elements = (JSONArray) model.getJSON("elements");
        if (elements == null || elements.isEmpty()) {
            return formatModelError(Language.L("表单布局尚未配置，请配置后使用"));
        }

        Record recordData = null;
        if (recordId != null) {
            recordData = findRecord(recordId, user, elements);
            if (recordData == null) {
                return formatModelError(Language.L("无权读取此记录或记录已被删除"));
            }
        }

        // 自动只读
        Set<String> roAutos = EasyMetaFactory.getAutoReadonlyFields(entity);
        Set<String> roAutosWithout = recordId == null ? null : Collections.emptySet();
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

        buildModelElements(elements, entityMeta, recordData, user, viewMode, true);

        if (elements.isEmpty()) {
            return formatModelError(Language.L("此表单布局尚未配置，请配置后使用"));
        }

        model.set("entityMeta", EasyMetaFactory.toJSON(entityMeta));

        // 主/明细实体处理
        if (hasMainEntity != null) {
            model.set("mainMeta", EasyMetaFactory.toJSON(hasMainEntity));
            // v3.4
            model.set("detailsNotEmpty", entityMeta.getExtraAttrs().getBooleanValue(EasyEntityConfigProps.DETAILS_NOTEMPTY));
            // v3.6
            model.set("detailsCopiable", entityMeta.getExtraAttrs().getBooleanValue(EasyEntityConfigProps.DETAILS_COPIABLE));
        } else if (entityMeta.getDetailEntity() != null) {
            model.set("detailMeta", EasyMetaFactory.toJSON(entityMeta.getDetailEntity()));
            // compatible v3.3
            model.set("detailsNotEmpty", entityMeta.getExtraAttrs().getBooleanValue(EasyEntityConfigProps.DETAILS_NOTEMPTY));
            // v3.4 N-D
            List<JSON> detailMetas = new ArrayList<>();
            for (Entity de : MetadataSorter.sortDetailEntities(entityMeta)) detailMetas.add(EasyMetaFactory.toJSON(de));
            model.set("detailMetas", detailMetas);
        }

        // 最后修改时间
        if (recordData != null && recordData.hasValue(EntityHelper.ModifiedOn)) {
            model.set("lastModified", recordData.getDate(EntityHelper.ModifiedOn).getTime());
        }

        if (approvalState != null) {
            model.set("hadApproval", approvalState.getState());

            // v3.4 无审批流程了
            if (approvalState.getState() >= ApprovalState.REJECTED.getState()) {
                boolean notHadApproval = !RobotApprovalManager.instance.hadApproval(hasMainEntity == null ? entityMeta : hasMainEntity);
                if (notHadApproval) model.set("hadApproval", null);
            }
        }

        if (readonlyMessage != null) model.set("readonlyMessage", readonlyMessage);

        // v3.4
        String disabledViewEditable = EasyMetaFactory.valueOf(entityMeta)
                .getExtraAttr(EasyEntityConfigProps.DISABLED_VIEW_EDITABLE);
        model.set("onViewEditable", !BooleanUtils.toBoolean(disabledViewEditable));
        
        // v3.7
        model.set("hadSop", true);

        model.set("layoutId", model.getID("id"));
        model.remove("id");
        // v4.0
        if (recordId != null && !EntityHelper.isUnsavedId(recordId)) {
            model.set("recordId", recordId);
            model.set("recordName", FieldValueHelper.getLabelNotry(recordId));
        }
        return model.toJSON();
    }

    /**
     * @param error
     * @return
     */
    private JSONObject formatModelError(String error) {
        return JSONUtils.toJSONObject("error", error);
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

        // 普通实体（非明细）
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
     * @param viewModel 是否视图
     * @param useAdvControl 是否使用表单高级控制
     */
    protected void buildModelElements(JSONArray elements, Entity entity, Record recordData, ID user, boolean viewModel, boolean useAdvControl) {
        final User formUser = Application.getUserStore().getUser(user);
        final Date now = CalendarUtils.now();

        // 新建
        final boolean isNew = recordData == null || recordData.getPrimary() == null
                || EntityHelper.isUnsavedId(recordData.getPrimary());

        final FieldPrivileges fp = Application.getPrivilegesManager().getFieldPrivileges();

        // Check and clean
        for (Iterator<Object> iter = elements.iterator(); iter.hasNext(); ) {
            JSONObject el = (JSONObject) iter.next();
            String fieldName = el.getString("field");
            if (DIVIDER_LINE.equalsIgnoreCase(fieldName)) continue;
            if (REFFORM_LINE.equalsIgnoreCase(fieldName)) {
                // v3.6
                if (viewModel && recordData != null) {
                    String reffield = el.getString("reffield");
                    Object v = recordData.getObjectValue(reffield);
                    if (v == null && entity.containsField(reffield)) {
                        v = Application.getQueryFactory().unique(recordData.getPrimary(), reffield)[0];
                    }
                    if (v != null) {
                        el.put("refvalue", new Object[]{ v, entity.getField(reffield).getReferenceEntity().getName() });
                    }
                }
                continue;
            }

            // 已删除字段
            if (!MetadataHelper.checkAndWarnField(entity, fieldName)) {
                iter.remove();
                continue;
            }

            // v2.2 高级控制
            // v3.8.4 视图下也有效（单字段编辑也算编辑）
            if (useAdvControl) {
                Object hiddenOnCreate = el.remove("hiddenOnCreate");
                Object hiddenOnUpdate = el.remove("hiddenOnUpdate");
                if (hiddenOnCreate == null) {
                    Object displayOnCreate39 = el.remove("displayOnCreate");
                    Object displayOnUpdate39 = el.remove("displayOnUpdate");
                    if (displayOnCreate39 != null && !(Boolean) displayOnCreate39) hiddenOnCreate = true;
                    if (displayOnUpdate39 != null && !(Boolean) displayOnUpdate39) hiddenOnUpdate = true;
                }
                final Object requiredOnCreate = el.remove("requiredOnCreate");
                final Object requiredOnUpdate = el.remove("requiredOnUpdate");
                final Object readonlyOnCreate = el.remove("readonlyOnCreate");
                final Object readonlyOnUpdate = el.remove("readonlyOnUpdate");
                // fix v3.3.4 跟随主记录新建/更新
                boolean isNewState = isNew;
                if (entity.getMainEntity() != null) {
                    ID fromMain = FormsBuilderContextHolder.getMainIdOfDetail(false);
                    isNewState = EntityHelper.isUnsavedId(fromMain);
                }

                // 视图下忽略此选项
                if (viewModel) {
                    hiddenOnCreate = false;
                    hiddenOnUpdate = false;
                }
                // 隐藏 v4.0
                if (hiddenOnCreate != null && (Boolean) hiddenOnCreate && isNewState) {
                    iter.remove();
                    continue;
                }
                if (hiddenOnUpdate != null && (Boolean) hiddenOnUpdate && !isNewState) {
                    iter.remove();
                    continue;
                }
                // 必填
                if (requiredOnCreate != null && (Boolean) requiredOnCreate && isNewState) {
                    el.put("nullable", false);
                }
                if (requiredOnUpdate != null && (Boolean) requiredOnUpdate && !isNewState) {
                    el.put("nullable", false);
                }
                // 只读 v3.6
                if (readonlyOnCreate != null && (Boolean) readonlyOnCreate && isNewState) {
                    el.put("readonly", true);
                }
                if (readonlyOnUpdate != null && (Boolean) readonlyOnUpdate && !isNewState) {
                    el.put("readonly", true);
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
                        easyField.getExtraAttr(EasyFieldConfigProps.DATETIME_FORMAT), dt.getDefaultFormat());
                el.put(EasyFieldConfigProps.DATETIME_FORMAT, format);
            } else if (dt == DisplayType.DATE) {
                String format = StringUtils.defaultIfBlank(
                        easyField.getExtraAttr(EasyFieldConfigProps.DATE_FORMAT), dt.getDefaultFormat());
                el.put(EasyFieldConfigProps.DATE_FORMAT, format);
            } else if (dt == DisplayType.TIME) {
                String format = StringUtils.defaultIfBlank(
                        easyField.getExtraAttr(EasyFieldConfigProps.TIME_FORMAT), dt.getDefaultFormat());
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

                if (dt == DisplayType.REFERENCE && License.isRbvAttached()) {
                    el.put("fillinWithFormData", true);
                }
            }

            // 新建记录
            if (isNew) {
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
                    if (dt == DisplayType.SERIES
                            || EntityHelper.ApprovalLastTime.equals(fieldName) || EntityHelper.ApprovalLastRemark.equals(fieldName)
                            || EntityHelper.ApprovalLastUser.equals(fieldName) || EntityHelper.ApprovalStepUsers.equals(fieldName)
                            || EntityHelper.ApprovalStepNodeName.equals(fieldName)) {
                        el.put("readonlyw", READONLYW_RO);
                    } else {
                        Object defaultValue = easyField.exprDefaultValue();
                        if (defaultValue != null) {
                            defaultValue = easyField.wrapValue(defaultValue);
                            // `wrapValue` 会添加格式符号
                            if (dt == DisplayType.DECIMAL || dt == DisplayType.NUMBER) {
                                defaultValue = EasyDecimal.clearFlaged(defaultValue);
                            }
                            el.put("value", defaultValue);
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
            }

            // 编辑/视图/记录转换
            if (recordData != null) {
                Object value = wrapFieldValue(recordData, easyField, user);
                if (value != null) {
                    // `wrapValue` 会添加格式符号
                    if (!viewModel && (dt == DisplayType.DECIMAL || dt == DisplayType.NUMBER)) {
                        value = EasyDecimal.clearFlaged(value);
                    }
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

            // Clean
            el.remove(EasyFieldConfigProps.ADV_PATTERN);
            el.remove(EasyFieldConfigProps.ADV_DESENSITIZED);
            el.remove("barcodeFormat");
            el.remove("seriesFormat");

            String decimalType = el.getString("decimalType");
            if (decimalType != null && decimalType.contains("%s")) {
                el.put("decimalType", decimalType.replace("%s", ""));
            }

            // v3.8 字段权限
            if (isNew) {
                if (!fp.isCreatable(fieldMeta, user)) el.put("readonly", true);
            } else {
                // v4.0 保留占位
                if (!fp.isReadable(fieldMeta, user)) {
                    el.put("unreadable", true);
                    el.put("readonly", true);
                    el.remove("value");
                }
                else if (!fp.isUpdatable(fieldMeta, user)) el.put("readonly", true);
            }
        }
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
     * @see DataListWrapper#wrapFieldValue(Object, Field)
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
        if (initialVal == null || initialVal.isEmpty()) return;

        JSONArray elements = ((JSONObject) formModel).getJSONArray("elements");
        if (elements == null || elements.isEmpty()) return;

        // v3.9 可携带明细
        Object hasDetails = initialVal.remove(GeneralEntityService.HAS_DETAILS);
        if (hasDetails instanceof JSONArray && !((JSONArray) hasDetails).isEmpty()) {
            JSONObject ds = setFormInitialValue4Details39(entity, (JSONArray) hasDetails);
            if (!ds.isEmpty()) ((JSONObject) formModel).put(GeneralEntityService.HAS_DETAILS, ds);
        }

        // 已布局字段。字段是否布局会影响返回值
        Set<String> inFormFields = new HashSet<>();
        for (Object o : elements) {
            inFormFields.add(((JSONObject) o).getString("field"));
        }

        // 保持在初始值中
        Set<String> initialValKeeps = new HashSet<>();

        Map<String, Object> initialValReady = new HashMap<>();
        for (Map.Entry<String, Object> e : initialVal.entrySet()) {
            final String field = e.getKey();
            Object v = e.getValue();
            if (!(v instanceof String)) {
                if (!(EntityRecordCreator.META_FIELD.equals(field))) {
                    log.warn("Invalid value in `initialVal` : {}", e);
                }
                continue;
            }
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

                // v3.9 明细直接新建
                if (DV_MAINID_FJS.equals(value)) {
                    for (Object o : elements) {
                        JSONObject item = (JSONObject) o;
                        if (dtmField.getName().equalsIgnoreCase(item.getString("field"))) {
                            item.remove("readonly");
                            item.remove("readonlyw");
                            break;
                        }
                    }
                } else if (mixValue != null) {
                    initialValReady.put(dtmField.getName(), mixValue);
                    initialValKeeps.add(dtmField.getName());
                }
            }
            // 其他
            else if (entity.containsField(field)) {
                final EasyField easyField = EasyMetaFactory.valueOf(entity.getField(field));
                final DisplayType dt = easyField.getDisplayType();
                if (dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE) {
                    // v3.4 如果字段设置了附加过滤条件，从相关项新建时要检查是否符合
                    if (!FieldValueHelper.checkRefDataFilter(easyField, ID.valueOf(value))) {
                        ((JSONObject) formModel).put("alertMessage",
                                Language.L("%s不符合附加过滤条件，不能自动填写", Language.L(easyField)));
                        continue;
                    }

                    Object mixValue = inFormFields.contains(field) ? getReferenceMixValue(value) : value;
                    if (mixValue != null) {
                        if (dt == DisplayType.REFERENCE) {
                            initialValReady.put(field, mixValue);
                        } else {
                            // N2N 是数组
                            initialValReady.put(field,
                                    inFormFields.contains(field) ? new Object[] { mixValue } : value);
                        }
                    }

                } else {
                    // v3.9 其他类型，需要限制字段类型???
                    initialValReady.put(field, value);
                }

            } else {
                log.warn("Unknown value pair : {} = {}", field, value);
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

    // 支持明细
    private JSONObject setFormInitialValue4Details39(Entity entity, JSONArray details) {
        final Entity defDetailEntity = entity.getDetailEntity();
        Assert.notNull(defDetailEntity, "None detail-entity");

        ID forceMainid = EntityHelper.UNSAVED_ID;
        FormsBuilderContextHolder.setMainIdOfDetail(forceMainid);
        JSONObject dsMap = new JSONObject();
        try {
            for (Object o : details) {
                JSONObject item = (JSONObject) o;
                JSONObject metadata = item.getJSONObject(EntityRecordCreator.META_FIELD);
                Entity detailEntity = null;
                if (metadata != null) {
                    String n = metadata.getString("entity");
                    if (n != null) detailEntity = MetadataHelper.getEntity(n);
                }
                if (detailEntity == null) detailEntity = defDetailEntity;

                // 新建明细记录时必须指定主实体
                item.put(DV_MAINID, forceMainid.toString());
                JSON model = buildForm(detailEntity.getName(), UserService.SYSTEM_USER, null);
                setFormInitialValue(detailEntity, model, item);

                JSONArray ds = dsMap.getJSONArray(detailEntity.getName());
                if (ds == null) {
                    ds = new JSONArray();
                    dsMap.put(detailEntity.getName(), ds);
                }
                ds.add(model);
            }

        } finally {
            FormsBuilderContextHolder.getMainIdOfDetail(true);
        }
        return dsMap;
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
            log.error("No record found : {}", idValue);
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
        return o != null && o[0] instanceof ID ? (ID) o[0] : null;
    }

    private boolean isNewMainId(Object id) {
        return DV_MAINID.equals(id) || EntityHelper.isUnsavedId(id);
    }
}
