/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.configuration.portals.PickListManager;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 字段值兼容转换
 *
 * @author ZHAO
 * @since 2020/2/8
 */
public class CompatibleValueConversion {

    private static final Log LOG = LogFactory.getLog(CompatibleValueConversion.class);

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
     * @return
     */
    public Object conversion(Object sourceValue) {
        return conversion(sourceValue, false);
    }

    /**
     * @param sourceValue
     * @param mixValue
     * @return
     */
    public Object conversion(Object sourceValue, boolean mixValue) {
        if (sourceValue == null || NullValue.is(sourceValue)) {
            return null;
        }

        final EasyMeta sourceField = EasyMeta.valueOf(source);
        final DisplayType sourceType = sourceField.getDisplayType();
        final DisplayType targetType = EasyMeta.getDisplayType(target);
        final boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;

        Object compatibleValue = sourceValue;
        if (sourceType == DisplayType.ID) {
            if (is2Text) {
                compatibleValue = sourceValue.toString().toUpperCase();
            }
        }
        else if (sourceType == DisplayType.REFERENCE) {
            if (is2Text) {
                compatibleValue = FieldValueWrapper.getLabelNotry((ID) sourceValue);
            } else if (mixValue) {
                String text = FieldValueWrapper.getLabelNotry((ID) sourceValue);
                compatibleValue = FieldValueWrapper.wrapMixValue((ID) sourceValue, text);
            }
        }
        else if (sourceType == DisplayType.CLASSIFICATION) {
            if (is2Text) {
                compatibleValue = FieldValueWrapper.instance.wrapFieldValue(sourceValue, sourceField, true);
            } else if (mixValue) {
                compatibleValue = FieldValueWrapper.instance.wrapFieldValue(sourceValue, sourceField, false);
            }
        }
        else if (sourceType == DisplayType.PICKLIST) {
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
        }
        else if (sourceType == DisplayType.STATE) {
            if (is2Text) {
                compatibleValue = FieldValueWrapper.instance.wrapState(sourceValue, sourceField);
            }
        }
        else if (sourceType == DisplayType.DATETIME && targetType == DisplayType.DATE) {
            String datetime = FieldValueWrapper.instance.wrapDatetime(sourceValue, sourceField);
            compatibleValue = datetime.split(" ")[0];
            if (!(is2Text || mixValue)) {
                compatibleValue = CalendarUtils.parse((String) compatibleValue);
            }
        }
        else if (sourceType == DisplayType.DATE && targetType == DisplayType.DATETIME) {
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
        }
        else if (is2Text) {
            compatibleValue = FieldValueWrapper.instance.wrapFieldValue(sourceValue, sourceField);
        }
        // 整数/浮点数无需转换，因为持久层框架已有兼容处理

        return compatibleValue;
    }

}
