/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MetadataException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.impl.FieldExtConfigProps;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.state.StateHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.text.DecimalFormat;

/**
 * 字段值包装。例如 BOOL 类型的 T/F 将格式化为 是/否。
 * 表单/视图/列表等均调用此类，仅在处理特定情景下的特定字段时才需要特殊处理
 *
 * @author zhaofang123@gmail.com
 * @since 09/23/2018
 */
public class FieldValueWrapper {

    /**
     * 引用值被删除时的默认显示
     */
    public static final String MISS_REF_PLACE = "[DELETED]";

    /**
     * 名称字段为空时，采用 @+ID 的方式显示
     */
    public static final String NO_LABEL_PREFIX = "@";

    public static final FieldValueWrapper instance = new FieldValueWrapper();

    private FieldValueWrapper() {
    }

    /**
     * @param value
     * @param field
     * @param unpackMix
     * @return
     * @see #wrapFieldValue(Object, EasyField, boolean)
     */
    public Object wrapFieldValue(Object value, Field field, boolean unpackMix) {
        return wrapFieldValue(value, EasyMetaFactory.valueOf(field), unpackMix);
    }

    /**
     * @param value
     * @param field
     * @param unpackMix
     * @return
     * @see #wrapFieldValue(Object, EasyField)
     */
    public Object wrapFieldValue(Object value, EasyField field, boolean unpackMix) {
        value = wrapFieldValue(value, field);
        if (unpackMix && value != null) {
            DisplayType dt = field.getDisplayType();
            if (value instanceof JSON && (dt == DisplayType.CLASSIFICATION || dt == DisplayType.REFERENCE)) {
                return ((JSONObject) value).getString("text");
            } else if (dt == DisplayType.FILE || dt == DisplayType.IMAGE) {
                return value.toString();
            }
        }
        return value;
    }

    /**
     * `REFERENCE` 和 `CLASSIFICATION` 返回复合值
     * `FILE` 和 `IMAGE` 返回 JSONArray
     * 其他返回格式化后的值
     *
     * @param value
     * @param field
     * @return
     */
    public Object wrapFieldValue(Object value, EasyField field) {
        Object useSpecial = wrapSpecialField(value, field);
        if (useSpecial != null) {
            return useSpecial;
        }

        if (value == null || StringUtils.isBlank(value.toString())) {
            return StringUtils.EMPTY;
        }

        DisplayType dt = field.getDisplayType();
        if (dt == DisplayType.DATE) {
            return wrapDate(value, field);
        } else if (dt == DisplayType.DATETIME) {
            return wrapDatetime(value, field);
        } else if (dt == DisplayType.NUMBER) {
            return wrapNumber(value, field);
        } else if (dt == DisplayType.DECIMAL) {
            return wrapDecimal(value, field);
        } else if (dt == DisplayType.REFERENCE) {
            return wrapReference(value, field);
        } else if (dt == DisplayType.N2NREFERENCE) {
            return wrapN2NReference(value, field);
        } else if (dt == DisplayType.BOOL) {
            return wrapBool(value, field);
        } else if (dt == DisplayType.PICKLIST) {
            return wrapPickList(value, field);
        } else if (dt == DisplayType.STATE) {
            return wrapState(value, field);
        } else if (dt == DisplayType.CLASSIFICATION) {
            return wrapClassification(value, field);
        } else if (dt == DisplayType.MULTISELECT) {
            return wrapMultiSelect(value, field);
        } else if (dt == DisplayType.IMAGE || dt == DisplayType.FILE) {
            return wrapFile(value, field);
        } else if (dt == DisplayType.AVATAR || dt == DisplayType.LOCATION) {
            return value;
        } else if (dt == DisplayType.BARCODE) {
            return wrapBarcode(value, field);
        } else {
            return wrapSimple(value, field);
        }
    }

    /**
     * @param value
     * @param field
     * @return
     */
    public String wrapDate(Object value, EasyField field) {
        String format = field.getExtraAttr(FieldExtConfigProps.DATE_DATEFORMAT);
        if (StringUtils.isBlank(format)) format = field.getDisplayType().getDefaultFormat();
        return CalendarUtils.getDateFormat(format).format(value);
    }

    /**
     * @param value
     * @param field
     * @return
     */
    public String wrapDatetime(Object value, EasyField field) {
        String format = field.getExtraAttr(FieldExtConfigProps.DATETIME_DATEFORMAT);
        if (StringUtils.isBlank(format)) format = field.getDisplayType().getDefaultFormat();
        return CalendarUtils.getDateFormat(format).format(value);
    }

    /**
     * @param value
     * @param field
     * @return
     */
    public String wrapNumber(Object value, EasyField field) {
        String format = field.getExtraAttr(FieldExtConfigProps.NUMBER_FORMAT);
        if (StringUtils.isBlank(format)) format = field.getDisplayType().getDefaultFormat();
        return new DecimalFormat(format).format(value);
    }

    /**
     * @param value
     * @param field
     * @return
     */
    public String wrapDecimal(Object value, EasyField field) {
        String format = field.getExtraAttr(FieldExtConfigProps.DECIMAL_FORMAT);
        if (StringUtils.isBlank(format)) format = field.getDisplayType().getDefaultFormat();
        return new DecimalFormat(format).format(value);
    }

    /**
     * @param value
     * @param field
     * @return
     * @see #wrapMixValue(ID, String)
     */
    public JSONObject wrapReference(Object value, EasyField field) {
        Object text = ((ID) value).getLabelRaw();
        if (text == null) {
            text = getLabelNotry((ID) value);

        } else {
            Field nameField = field.getRawMeta().getReferenceEntity().getNameField();
            text = instance.wrapFieldValue(text, nameField, true);
        }

        return wrapMixValue((ID) value, text == null ? null : text.toString());
    }

    /**
     * @param value
     * @param field
     * @return
     * @see #wrapReference(Object, EasyField)
     */
    public JSONArray wrapN2NReference(Object value, EasyField field) {
        ID[] ids = (ID[]) value;
        
        JSONArray idArray = new JSONArray();
        for (ID id : ids) {
            idArray.add(wrapReference(id, field));
        }
        return idArray;
    }

    /**
     * @param value
     * @param field
     * @return
     */
    public String wrapBool(Object value, EasyField field) {
        return (Boolean) value ? "是" : "否";
    }

    /**
     * @param value
     * @param field
     * @return
     * @see PickListManager
     */
    public String wrapPickList(Object value, EasyField field) {
        return StringUtils.defaultIfBlank(PickListManager.instance.getLabel((ID) value), MISS_REF_PLACE);
    }

    /**
     * @param value
     * @param field
     * @return
     */
    public String wrapState(Object value, EasyField field) {
        String stateClass = field.getExtraAttr(FieldExtConfigProps.STATE_STATECLASS);
        return Language.L(StateHelper.valueOf(stateClass, (Integer) value));
    }

    /**
     * @param value
     * @param field
     * @return
     * @see ClassificationManager
     */
    public JSON wrapClassification(Object value, EasyField field) {
        ID id = (ID) value;
        String text = StringUtils.defaultIfBlank(ClassificationManager.instance.getFullName(id), MISS_REF_PLACE);
        return wrapMixValue(id, text);
    }

    /**
     * @param value
     * @param field
     * @return
     * @see MultiSelectManager
     */
    public String wrapMultiSelect(Object value, EasyField field) {
        if ((Long) value <= 0) {
            return StringUtils.EMPTY;
        }
        String[] multiLabel = MultiSelectManager.instance.getLabel((Long) value, field.getRawMeta());
        return StringUtils.join(multiLabel, " / ");
    }

    /**
     * @param value
     * @param field
     * @return
     */
    public JSON wrapFile(Object value, EasyField field) {
        return JSON.parseArray(value.toString());
    }

    /**
     * BARCODE 为动态值
     *
     * @param value 必须为记录ID
     * @param field
     * @return
     * @see BarCodeGenerator
     */
    public String wrapBarcode(Object value, EasyField field) {
        if (value instanceof ID) {
            return BarCodeGenerator.getBarCodeContent(field.getRawMeta(), (ID) value);
        }
        return null;
    }

    /**
     * @param value
     * @param field
     * @return
     */
    public String wrapSimple(Object value, EasyField field) {
        String text = value.toString().trim();
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        } else {
            return text;
        }
    }

    /**
     * 特殊字段处理
     *
     * @param value
     * @param field
     * @return
     */
    protected Object wrapSpecialField(Object value, EasyField field) {
        if (!field.isQueryable()) {
            return "******";
        }

        // 审批
        if (field.getName().equalsIgnoreCase(EntityHelper.ApprovalState)) {
            if (value == null) {
                return Language.L(ApprovalState.DRAFT);
            } else {
                return Language.L(ApprovalState.valueOf((Integer) value));
            }

        } else if (field.getName().equalsIgnoreCase(EntityHelper.ApprovalId) && value == null) {
            return wrapMixValue(null, Language.L(ApprovalState.DRAFT));
        }

        return null;
    }

    // --

    /**
     * 获取记录的 NAME/LABEL 字段值
     *
     * @param id
     * @param defaultValue
     * @return
     * @throws NoRecordFoundException If no record found
     */
    public static String getLabel(ID id, String defaultValue) throws NoRecordFoundException {
        Assert.notNull(id, "[id] cannot be null");
        Entity entity = MetadataHelper.getEntity(id.getEntityCode());

        if (id.getEntityCode() == EntityHelper.ClassificationData) {
            String hasValue = ClassificationManager.instance.getFullName(id);
            if (hasValue == null) {
                throw new NoRecordFoundException("No ClassificationData found by id : " + id);
            }
            return hasValue;
        } else if (id.getEntityCode() == EntityHelper.PickList) {
            String hasValue = PickListManager.instance.getLabel(id);
            if (hasValue == null) {
                throw new NoRecordFoundException("No PickList found by id : " + id);
            }
            return hasValue;
        }

        Field nameField = MetadataHelper.getNameField(entity);
        Object[] nameValue = Application.getQueryFactory().uniqueNoFilter(id, nameField.getName());
        if (nameValue == null) {
            throw new NoRecordFoundException("No record found by id : " + id);
        }

        Object nameLabel = instance.wrapFieldValue(nameValue[0], nameField, true);
        if (nameLabel == null || StringUtils.isBlank(nameLabel.toString())) {
            if (defaultValue == null) {
                defaultValue = NO_LABEL_PREFIX + id.toLiteral().toUpperCase();
            }
            return defaultValue;
        }
        return nameLabel.toString();
    }

    /**
     * @param id
     * @return
     * @throws NoRecordFoundException
     */
    public static String getLabel(ID id) throws NoRecordFoundException {
        return getLabel(id, null);
    }

    /**
     * @param id
     * @return
     * @see #getLabel(ID)
     */
    public static String getLabelNotry(ID id) {
        try {
            return getLabel(id);
        } catch (MetadataException | NoRecordFoundException ex) {
            return MISS_REF_PLACE;
        }
    }

    /**
     * @param id
     * @param text
     * @return Returns `{ id:xxx, text:xxx, entity:xxx }`
     */
    public static JSONObject wrapMixValue(ID id, String text) {
        if (id != null && StringUtils.isBlank(text)) {
            text = id.getLabel();
        }

        JSONObject o = JSONUtils.toJSONObject(new String[]{"id", "text"}, new Object[]{id, text});
        if (id != null) {
            o.put("entity", MetadataHelper.getEntityName(id));
        }
        return o;
    }
}
