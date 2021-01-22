/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;

/**
 * RB 封装字段类型
 *
 * @author zhaofang123@gmail.com
 * @since 05/18/2018
 */
public enum DisplayType {

    NUMBER(EasyNumber.class, "整数", FieldType.LONG, -1, "##,###"),
    DECIMAL(EasyDecimal.class, "货币", FieldType.DECIMAL, -1, "##,##0.00"),
    DATE(EasyDate.class, "日期", FieldType.DATE, -1, "yyyy-MM-dd"),
    DATETIME(EasyDateTime.class, "日期时间", FieldType.TIMESTAMP, -1, "yyyy-MM-dd HH:mm:ss"),
    TEXT(EasyText.class, "文本", FieldType.STRING, 200, null),
    NTEXT(EasyNText.class, "多行文本", FieldType.TEXT, 30000, null),
    EMAIL(EasyEmail.class, "邮箱", FieldType.STRING, 100, null),
    URL(EasyUrl.class, "链接", FieldType.STRING, 200, null),
    PHONE(EasyPhone.class, "电话", FieldType.STRING, 40, null),
    SERIES(EasySeries.class, "自动编号", FieldType.STRING, 40, "{YYYYMMDD}-{0000}"),
    IMAGE(EasyImage.class, "图片", FieldType.STRING, 700, null),
    FILE(EasyFile.class, "附件", FieldType.STRING, 700, null),
    PICKLIST(EasyPickList.class, "选项", FieldType.REFERENCE, -1, null),
    CLASSIFICATION(EasyClassification.class, "分类", FieldType.REFERENCE, -1, null),
    REFERENCE(EasyReference.class, "引用", FieldType.REFERENCE, -1, null),
    AVATAR(EasyAvatar.class, "头像", FieldType.STRING, 300, null),
    MULTISELECT(EasyMultiSelect.class, "多选", FieldType.LONG, -1, null),
    BOOL(EasyBool.class, "布尔", FieldType.BOOL, -1, null),
    BARCODE(EasyBarCode.class, "条形码", FieldType.STRING, 300, null),
    N2NREFERENCE(EasyN2NReference.class, "多引用", FieldType.REFERENCE_LIST, -1, null),

    // 内部

    ID(EasyID.class, "主键", FieldType.PRIMARY, -1, null),
    STATE(EasyState.class, "状态", FieldType.SMALL_INT, -1, null),
    ANYREFERENCE(EasyAnyReference.class, "任意引用", FieldType.ANY_REFERENCE, -1, null),
    LOCATION(EasyLocation.class, "位置", FieldType.STRING, 70, null),

    ;

    // --

    private final Class<? extends EasyField> easyClass;
    private final String displayName;
    private final Type fieldType;
    private final String defaultFormat;
    private final int maxLength;

    DisplayType(Class<? extends EasyField> easyClass, String displayName, Type fieldType, int maxLength, String defaultFormat) {
        this.easyClass = easyClass;
        this.displayName = displayName;
        this.fieldType = fieldType;
        this.defaultFormat = defaultFormat;
        this.maxLength = maxLength;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Type getFieldType() {
        return fieldType;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public String getDefaultFormat() {
        return defaultFormat;
    }

    protected Class<? extends EasyField> getEasyClass() {
        return easyClass;
    }

    @Override
    public String toString() {
        return getDisplayName() + " (" + name().toUpperCase() + ")";
    }
}
