/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MetadataException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.MixValue;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.approval.ApprovalStepService;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.DataDesensitized;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字段值包装
 *
 * @author Zixin (RB)
 * @since 09/23/2018
 */
@Slf4j
public class FieldValueHelper {

    /**
     * 引用值被删除时的默认显示
     */
    public static final String MISS_REF_PLACE = "[DELETED]";
    /**
     * 名称字段为空时，采用 @+ID 的方式显示
     */
    public static final String NO_LABEL_PREFIX = "@";
    /**
     * 无权限标识
     */
    public static final String NO_READ_PRIVILEGES = "$NOPRIVILEGES$";

    /**
     * @param value
     * @param field
     * @param unpackMix
     * @return
     */
    public static Object wrapFieldValue(Object value, Field field, boolean unpackMix) {
        return wrapFieldValue(value, EasyMetaFactory.valueOf(field), unpackMix);
    }

    /**
     * @param value
     * @param field
     * @param unpackMix
     * @return
     */
    public static Object wrapFieldValue(Object value, EasyField field, boolean unpackMix) {
        value = wrapFieldValue(value, field);
        if (unpackMix && value != null && field instanceof MixValue) {
            return ((MixValue) field).unpackWrapValue(value);
        }
        return value;
    }

    /**
     * @param value
     * @param field
     * @return
     * @see EasyField#wrapValue(Object)
     */
    public static Object wrapFieldValue(Object value, EasyField field) {
        final DisplayType dt = field.getDisplayType();

        if (value != null && !field.isQueryable() &&
                (dt == DisplayType.TEXT || dt == DisplayType.NTEXT)) {
            return DataDesensitized.SECURE_TEXT;
        }

        // 空值: 默认值
        if (!CommonsUtils.hasLength(value)) {
            if (field.getName().equalsIgnoreCase(EntityHelper.ApprovalState)) {
                return ApprovalState.DRAFT.getState();
            } else if (field.getName().equalsIgnoreCase(EntityHelper.ApprovalId)) {
                return wrapMixValue(null, Language.L("未提交"));
            }

            return null;
        }

        // 非 ID 数组表示记录主键
        if (dt == DisplayType.N2NREFERENCE && value instanceof ID) {
            value = N2NReferenceSupport.items(field.getRawMeta(), (ID) value);
        }

        try {
            return field.wrapValue(value);
        } catch (JSONException ex) {
            log.error("Bad JSONArray format : {} < {}", value, field.getRawMeta());
            return JSONUtils.EMPTY_ARRAY;
//            throw new RebuildException("BAD VALUE FORMAT:" + value);
        }
    }

    /**
     * @param id
     * @param text
     * @return Returns `{ id:xxx, text:xxx [, entity:xxx] }`
     */
    public static JSONObject wrapMixValue(ID id, String text) {
        if (id != null && StringUtils.isBlank(text)) {
            text = id.getLabel();
        }

        JSONObject mixValue = JSONUtils.toJSONObject(
                new String[] { "id", "text" }, new Object[] { id, text });
        if (id != null) {
            if (!EntityHelper.isUnsavedId(id)) {
                if (MetadataHelper.containsEntity(id.getEntityCode())) {
                    mixValue.put("entity", MetadataHelper.getEntityName(id));
                } else {
                    log.warn("The entity of id no longer exists : {}", id);
                }
            } else if (ApprovalStepService.APPROVAL_NOID.equals(id)) {
                mixValue.put("entity", "RobotApprovalConfig");
            }
        }
        return mixValue;
    }

    /**
     * 获取记录的 NAME/LABEL 字段值
     *
     * @param id
     * @param default4Null
     * @return
     * @throws NoRecordFoundException If no record found
     */
    public static String getLabel(ID id, String default4Null) throws NoRecordFoundException {
        Assert.notNull(id, "[id] cannot be null");
        // v3.6
        if (EntityHelper.isUnsavedId(id)) return Language.L("新的");

        final Entity entity = MetadataHelper.getEntity(id.getEntityCode());

        if (id.getEntityCode() == EntityHelper.ClassificationData) {
            String hasValue = ClassificationManager.instance.getFullName(id);
            if (hasValue == null) throw new NoRecordFoundException("No ClassificationData found by id : " + id);
            return hasValue;
        } else if (id.getEntityCode() == EntityHelper.PickList) {
            String hasValue = PickListManager.instance.getLabel(id);
            if (hasValue == null) throw new NoRecordFoundException("No PickList found by id : " + id);
            return hasValue;
        } else if (id.equals(ApprovalStepService.APPROVAL_NOID)) {
            return Language.L("自动审批");
        } else if (MetadataHelper.isBizzEntity(id.getEntityCode())) {
            String hasName = UserHelper.getName(id);
            if (hasName == null) throw new NoRecordFoundException("No Bizz found by id : " + id);
            return hasName;
        }

        Field nameField = entity.getNameField();
        Object[] nameValue = Application.getQueryFactory().uniqueNoFilter(id, nameField.getName());
        if (nameValue == null) throw new NoRecordFoundException(id);

        Object nameLabel = wrapFieldValue(nameValue[0], nameField, true);
        if (!CommonsUtils.hasLength(nameLabel)) {
            if (default4Null == null) {
                default4Null = NO_LABEL_PREFIX + id.toLiteral().toUpperCase();
            }
            return default4Null;
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

    // 日期公式 {NOW+1D}
    private static final Pattern PATT_DATE = Pattern.compile("\\{NOW([-+])([0-9]{1,9})([YMDHI])}");
    /**
     * 解析日期表达式
     *
     * @param dateExpr
     * @param base
     * @return
     */
    public static Date parseDateExpr(String dateExpr, Date base) {
        if ("{NOW}".equals(dateExpr)) {
            return CalendarUtils.now();
        }

        Matcher m = PATT_DATE.matcher(StringUtils.remove(dateExpr, " "));
        if (m.matches()) {
            base = base == null ? CalendarUtils.now() : base;

            String op = m.group(1);
            String num = m.group(2);
            String unit = m.group(3);
            int num2int = ObjectUtils.toInt(num);
            if ("-".equals(op)) {
                num2int = -num2int;
            }

            Date date = null;
            if (num2int == 0) {
                date = base;
            } else if ("Y".equals(unit)) {
                date = CalendarUtils.add(base, num2int, Calendar.YEAR);
            } else if ("M".equals(unit)) {
                date = CalendarUtils.add(base, num2int, Calendar.MONTH);
            } else if ("D".equals(unit)) {
                date = CalendarUtils.add(base, num2int, Calendar.DAY_OF_MONTH);
            } else if ("H".equals(unit)) {
                date = CalendarUtils.add(base, num2int, Calendar.HOUR_OF_DAY);
            } else if ("I".equals(unit)) {
                date = CalendarUtils.add(base, num2int, Calendar.MINUTE);
            }
            return date;
        }

        return null;
    }

    /**
     * 是否脱敏
     *
     * @param field
     * @param user
     * @return
     */
    public static boolean isUseDesensitized(EasyField field, ID user) {
        if (user == null) {
            log.warn("No [user] spec! Cannot check desensitized");
            return false;
        }

        if (field.isDesensitized()) {
            if (UserHelper.isAdmin(user) && RebuildConfiguration.getBool(ConfigurationItem.SecurityEnhanced)) {
                return true;
            } else {
                return !Application.getPrivilegesManager().allow(user, ZeroEntry.AllowNoDesensitized);
            }
        }
        return false;
    }

    /**
     * 字段值脱敏。仅适用文本/邮箱/电话/数字字段
     *
     * @param field
     * @param value
     * @return
     */
    public static Object desensitized(EasyField field, Object value) {
        if (value == null) return null;

        DisplayType dt = field.getDisplayType();
        if (dt == DisplayType.EMAIL) {
            return DataDesensitized.email((String) value);
        } else if (dt == DisplayType.PHONE) {
            return DataDesensitized.phone((String) value);
        } else if (dt == DisplayType.TEXT) {
            return DataDesensitized.any((String) value);
        } else if (dt == DisplayType.DECIMAL || dt == DisplayType.NUMBER) {
            return DataDesensitized.SECURE_TEXT;
        } else {
            return value;
        }
    }

    /**
     * 字段值脱敏：复合值
     *
     * @param field
     * @param mixValue
     */
    public static void desensitizedMixValue(EasyField field, JSON mixValue) {
        if (mixValue instanceof JSONObject) {
            String text = ((JSONObject) mixValue).getString("text");
            if (text != null) {
                ((JSONObject) mixValue).put("text", FieldValueHelper.desensitized(field, text));
            }
        }
        // N2N
        else if (mixValue instanceof JSONArray) {
            for (Object o : (JSONArray) mixValue) desensitizedMixValue(field, (JSON) o);
        }
    }

    /**
     * 引用,多引用 附加过滤条件
     * TODO 快速新建时回填也应该检查 ???
     *
     * @param field
     * @param value
     * @return
     */
    public static boolean checkRefDataFilter(EasyField field, ID value) {
        String dataFilter = field.getExtraAttr(EasyFieldConfigProps.REFERENCE_DATAFILTER);
        if (!JSONUtils.wellFormat(dataFilter)) return true;

        JSONObject dataFilterJson = JSON.parseObject(dataFilter);
        if (!ParseHelper.validAdvFilter(dataFilterJson)) return true;

        return QueryHelper.isMatchAdvFilter(value, dataFilterJson);
    }
}
