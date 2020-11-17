/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.service.trigger.RobotTriggerManager;

import java.util.Set;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyField extends BaseEasyMeta<Field> {
    private static final long serialVersionUID = 6027165766338449527L;

    protected EasyField(Field field) {
        super(field);
    }

    @Override
    public String getLabel() {
        return getRawMeta().getType() == FieldType.PRIMARY ? "ID" : super.getLabel();
    }

    @Override
    public boolean isUpdatable() {
        if (!getRawMeta().isUpdatable()) return false;

        Field field = getRawMeta();
        if (field.getType() == FieldType.REFERENCE) {
            Set<String> set = RobotTriggerManager.instance.getAutoReadonlyFields(field.getOwnEntity().getName());
            return !set.contains(field.getName());
        }
        return true;
    }

    @Override
    public boolean isBuiltin() {
        if (super.isBuiltin()) return true;

        Field field = getRawMeta();
        if (MetadataHelper.isCommonsField(field)) return true;

        if (field.getType() == FieldType.REFERENCE) {
            // 明细-引用主记录的字段也是内建
            // @see MetadataHelper#getDetailToMainField
            Entity hasMain = field.getOwnEntity().getMainEntity();
            return hasMain != null && hasMain.equals(field.getReferenceEntity()) && !field.isCreatable();
        }
        return false;
    }

    /**
     * @param fullName
     * @return
     */
    public String getDisplayType(boolean fullName) {
        DisplayType dt = getDisplayType();
        if (fullName) {
            return dt.getDisplayName() + " (" + dt.name() + ")";
        } else {
            return dt.name();
        }
    }

    /**
     * @return
     */
    public DisplayType getDisplayType() {
        String displayType = getExtraAttr("displayType");
        DisplayType dt = displayType != null
                ? DisplayType.valueOf(displayType) : convertBuiltinFieldType(getRawMeta());
        if (dt != null) return dt;

        throw new RebuildException("Unsupported field type : " + getRawMeta());
    }

    /**
     * 将字段类型转成 DisplayType
     *
     * @param field
     * @return
     */
    private DisplayType convertBuiltinFieldType(Field field) {
        Type ft = field.getType();
        if (ft == FieldType.PRIMARY) {
            return DisplayType.ID;
        } else if (ft == FieldType.REFERENCE) {
            int typeCode = field.getReferenceEntity().getEntityCode();
            if (typeCode == EntityHelper.PickList) {
                return DisplayType.PICKLIST;
            } else if (typeCode == EntityHelper.Classification) {
                return DisplayType.CLASSIFICATION;
            } else {
                return DisplayType.REFERENCE;
            }
        } else if (ft == FieldType.ANY_REFERENCE) {
            return DisplayType.ANYREFERENCE;
        } else if (ft == FieldType.REFERENCE_LIST) {
            return DisplayType.N2NREFERENCE;
        } else if (ft == FieldType.TIMESTAMP) {
            return DisplayType.DATETIME;
        } else if (ft == FieldType.DATE) {
            return DisplayType.DATE;
        } else if (ft == FieldType.STRING) {
            return DisplayType.TEXT;
        } else if (ft == FieldType.TEXT || ft == FieldType.NTEXT) {
            return DisplayType.NTEXT;
        } else if (ft == FieldType.BOOL) {
            return DisplayType.BOOL;
        } else if (ft == FieldType.INT || ft == FieldType.SMALL_INT || ft == FieldType.LONG) {
            return DisplayType.NUMBER;
        } else if (ft == FieldType.DOUBLE || ft == FieldType.DECIMAL) {
            return DisplayType.DECIMAL;
        }
        return null;
    }

//    // 转换兼容值
//    abstract Object convertCompatibleValue(EasyField target, Object sourceValue);
//    // 转换符合字段类型的值
//    abstract Object checkoutValue(Object rawValue);
//    // 默认值
//    abstract Object exprDefaultValue(String expr);
}
