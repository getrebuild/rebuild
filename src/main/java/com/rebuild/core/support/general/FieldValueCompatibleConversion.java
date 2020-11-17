/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.DisplayType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 字段值兼容转换。
 * 注意：某些字段转换可能导致精度损失，例如浮点数转整数、多行文本转文本
 *
 * @author ZHAO
 * @since 2020/2/8
 */
public class FieldValueCompatibleConversion {

    private static final Logger LOG = LoggerFactory.getLogger(FieldValueCompatibleConversion.class);

    final private Field source;
    final private Field target;

    /**
     * @param sourceField
     * @param targetField
     */
    public FieldValueCompatibleConversion(Field sourceField, Field targetField) {
        this.source = sourceField;
        this.target = targetField;
    }

    /**
     * @param sourceValue
     * @return
     */
    public Object convert(Object sourceValue) {
        return convert(sourceValue, null, false);
    }

    /**
     * @param sourceValue
     * @param valueExpr
     * @return
     */
    public Object convert(Object sourceValue, String valueExpr) {
        return convert(sourceValue, valueExpr, false);
    }

    /**
     * @param sourceValue
     * @param valueExpr
     * @param returnMixValue
     * @return
     */
    public Object convert(Object sourceValue, String valueExpr, boolean returnMixValue) {
        if (sourceValue == null || NullValue.is(sourceValue)) {
            return null;
        }

        final EasyField sourceField = EasyMetaFactory.valueOf(source);
        final DisplayType sourceType = sourceField.getDisplayType();
        final DisplayType targetType = EasyMetaFactory.getDisplayType(target);
        final boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;

        // 日期公式
        if (StringUtils.isNotBlank(valueExpr)
                && (sourceType == DisplayType.DATETIME || sourceType == DisplayType.DATE)) {
            Date newDate = FieldDefaultValueHelper.parseDateExpr("{NOW" + valueExpr + "}", (Date) sourceValue);
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
            } else if (returnMixValue) {
                String text = FieldValueWrapper.getLabelNotry((ID) sourceValue);
                compatibleValue = FieldValueWrapper.wrapMixValue((ID) sourceValue, text);
            }

        } else if (sourceType == DisplayType.N2NREFERENCE) {
            if (is2Text) {
                ID[] idArray = (ID[]) sourceValue;
                List<String> texts = new ArrayList<>();
                for (ID id : idArray) {
                    texts.add(FieldValueWrapper.getLabelNotry(id));
                }
                compatibleValue = StringUtils.join(texts, ", ");

            } else if (returnMixValue) {
                compatibleValue = FieldValueWrapper.instance.wrapN2NReference(sourceValue, sourceField);
            }

        } else if (sourceType == DisplayType.CLASSIFICATION) {
            if (is2Text) {
                compatibleValue = FieldValueWrapper.instance.wrapFieldValue(sourceValue, sourceField, true);
            } else if (returnMixValue) {
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

            if (!returnMixValue) {
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

            if (!returnMixValue) {
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
