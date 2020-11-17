/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.trigger.RobotTriggerManager;

import java.util.Set;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public abstract class EasyField extends BaseEasyMeta<Field> {
    private static final long serialVersionUID = 6027165766338449527L;

    private final DisplayType displayType;

    protected EasyField(Field field, DisplayType displayType) {
        super(field);
        this.displayType = displayType;
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
     * 获取 RB 封装类型
     *
     * @return
     */
    public DisplayType getDisplayType() {
        return displayType;
    }

//    // 转换兼容值
//    abstract Object convertCompatibleValue(EasyField target, Object sourceValue);
//    // 转换符合字段类型的值
//    abstract Object checkoutValue(Object rawValue);
//    // 默认值
//    abstract Object exprDefaultValue(String expr);
}
