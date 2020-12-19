/*
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
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.approval.ApprovalStepService;
import com.rebuild.core.support.i18n.Language;
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
 * @author zhaofang123@gmail.com
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
     * 加密显示
     */
    public static final String SECURE_TEXT = "******";

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
        if (!field.isQueryable() &&
                (field.getDisplayType() == DisplayType.TEXT || field.getDisplayType() == DisplayType.NTEXT)) return SECURE_TEXT;

        if (value == null || StringUtils.isBlank(value.toString())) {
            // 审批
            if (field.getName().equalsIgnoreCase(EntityHelper.ApprovalState)) {
                return ApprovalState.DRAFT.getState();
            } else if (field.getName().equalsIgnoreCase(EntityHelper.ApprovalId)) {
                return wrapMixValue(null, Language.L(ApprovalState.DRAFT));
            }

            return null;
        }

        return field.wrapValue(value);
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
            if (MetadataHelper.containsEntity(id.getEntityCode())) {
                mixValue.put("entity", MetadataHelper.getEntityName(id));
            } else {
                log.warn("Entity no longer exists : {}", id);
            }
        }
        return mixValue;
    }

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

        } else if (id.equals(ApprovalStepService.APPROVAL_NOID)) {
            return Language.L("AUTOAPPROVAL");
        }

        Field nameField = entity.getNameField();
        Object[] nameValue = Application.getQueryFactory().uniqueNoFilter(id, nameField.getName());
        if (nameValue == null) {
            throw new NoRecordFoundException("No record found by id : " + id);
        }

        Object nameLabel = wrapFieldValue(nameValue[0], nameField, true);
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
}
