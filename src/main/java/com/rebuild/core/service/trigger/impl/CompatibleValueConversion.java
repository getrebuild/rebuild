/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.DefaultValueHelper;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.support.general.FieldValueWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 字段值兼容转换
 *
 * @author ZHAO
 * @since 2020/2/8
 */
public class CompatibleValueConversion {

    private static final Logger LOG = LoggerFactory.getLogger(CompatibleValueConversion.class);

    final private Field source;
    final private Field target;

    /**
     * @param sourceField
     * @param targetField
     */
    public CompatibleValueConversion(Field sourceField, Field targetField) {
        this.source = sourceField;
        this.target = targetField;
    }

    /**
     * @param sourceValue
     * @param appendExpr
     * @return
     */
    public Object conversion(Object sourceValue, String appendExpr) {
        return conversion(sourceValue, appendExpr, false);
    }

    /**
     * @param sourceValue
     * @param appendExpr
     * @param mixValue
     * @return
     */
    public Object conversion(Object sourceValue, String appendExpr, boolean mixValue) {
        if (sourceValue == null || NullValue.is(sourceValue)) {
            return null;
        }

        final EasyMeta sourceField = EasyMeta.valueOf(source);
        final DisplayType sourceType = sourceField.getDisplayType();
        final DisplayType targetType = EasyMeta.getDisplayType(target);
        final boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;

        // 日期公式
        if (appendExpr != null && (sourceType == DisplayType.DATETIME || sourceType == DisplayType.DATE)) {
            Date newDate = DefaultValueHelper.parseDateExpr("{NOW" + appendExpr + "}", (Date) sourceValue);
            if (newDate != null) {
                sourceValue = newDate;
            }
        }

        Object compatibleValue = sourceValue;
        if (sourceType == DisplayType.ID) {
            if (is2Text) {
                compatibleValue = sourceValue.toString().toUpperCase();
            }
        } else if (sourceType == DisplayType.REFERENCE) {
            if (is2Text) {
                compatibleValue = FieldValueWrapper.getLabelNotry((ID) sourceValue);
            } else if (mixValue) {
                String text = FieldValueWrapper.getLabelNotry((ID) sourceValue);
                compatibleValue = FieldValueWrapper.wrapMixValue((ID) sourceValue, text);
            }
        } else if (sourceType == DisplayType.CLASSIFICATION) {
            if (is2Text) {
                compatibleValue = FieldValueWrapper.instance.wrapFieldValue(sourceValue, sourceField, true);
            } else if (mixValue) {
                compatibleValue = FieldValueWrapper.instance.wrapFieldValue(sourceValue, sourceField, false);
            }
        } else if (sourceType == DisplayType.PICKLIST) {
            String text = FieldValueWrapper.instance.wrapPickList(sourceValue, sourceField);
            if (is2Text) {
                compatibleValue = text;
            } else {
                // 转换 PickList ID
                compatibleValue = PickListManager.instance.findItemByLabel(text, target);
                if (compatibleValue == null) {
                    LOG.warn("Cannot find value of PickList : " + text + " << " + target);
                }
            }
        } else if (sourceType == DisplayType.STATE) {
            if (is2Text) {
                compatibleValue = FieldValueWrapper.instance.wrapState(sourceValue, sourceField);
            }
        } else if (sourceType == DisplayType.DATETIME && targetType == DisplayType.DATE) {
            String datetime = FieldValueWrapper.instance.wrapDatetime(sourceValue, sourceField);
            compatibleValue = datetime.split(" ")[0];
            if (!(is2Text || mixValue)) {
                compatibleValue = CalendarUtils.parse((String) compatibleValue);
            }
        } else if (sourceType == DisplayType.DATE && targetType == DisplayType.DATETIME) {
            String date = FieldValueWrapper.instance.wrapDate(sourceValue, sourceField);
            if (date.length() == 4) {  // YYYY
                compatibleValue = date + "01-01 00:00:00";
            } else if (date.length() == 7) {  // YYYY-MM
                compatibleValue = date + "-01 00:00:00";
            } else {
                compatibleValue = date + " 00:00:00";
            }

            if (!(is2Text || mixValue)) {
                compatibleValue = CalendarUtils.parse((String) compatibleValue);
            }
        } else if (is2Text) {
            compatibleValue = FieldValueWrapper.instance.wrapFieldValue(sourceValue, sourceField);

        } else if (sourceType == DisplayType.NUMBER && targetType == DisplayType.DECIMAL) {
            compatibleValue = BigDecimal.valueOf((Long) sourceValue);

        } else if (sourceType == DisplayType.DECIMAL && targetType == DisplayType.NUMBER) {
            compatibleValue = ObjectUtils.toLong(sourceValue);

        }

        return compatibleValue;
    }

}
