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

    NUMBER(EasyNumber.class, "整数", FieldType.LONG, FieldType.NO_NEED_LENGTH, "##,###"),
    DECIMAL(EasyDecimal.class, "货币", FieldType.DECIMAL, FieldType.NO_NEED_LENGTH, "##,##0.00"),
    DATE(EasyDate.class, "日期", FieldType.DATE, FieldType.NO_NEED_LENGTH, "yyyy-MM-dd"),
    DATETIME(EasyDateTime.class, "日期时间", FieldType.TIMESTAMP, FieldType.NO_NEED_LENGTH, "yyyy-MM-dd HH:mm:ss"),
    TEXT(EasyText.class, "文本", FieldType.STRING, 200, null),
    NTEXT(EasyNText.class, "多行文本", FieldType.TEXT, 32767, null),
    EMAIL(EasyEmail.class, "邮箱", FieldType.STRING, 100, null),
    URL(EasyUrl.class, "链接", FieldType.STRING, 200, null),
    PHONE(EasyPhone.class, "电话", FieldType.STRING, 40, null),
    SERIES(EasySeries.class, "自动编号", FieldType.STRING, 40, "{YYYYMMDD}-{0000}"),
    IMAGE(EasyImage.class, "图片", FieldType.STRING, 700, null, true, false),
    FILE(EasyFile.class, "附件", FieldType.STRING, 700, null, true, false),
    PICKLIST(EasyPickList.class, "下拉列表", FieldType.REFERENCE, FieldType.NO_NEED_LENGTH, null),
    CLASSIFICATION(EasyClassification.class, "分类", FieldType.REFERENCE, FieldType.NO_NEED_LENGTH, null),
    REFERENCE(EasyReference.class, "引用", FieldType.REFERENCE, FieldType.NO_NEED_LENGTH, null),
    AVATAR(EasyAvatar.class, "头像", FieldType.STRING, 300, null, true, false),
    MULTISELECT(EasyMultiSelect.class, "多选", FieldType.LONG, FieldType.NO_NEED_LENGTH, null),
    BOOL(EasyBool.class, "布尔", FieldType.BOOL, FieldType.NO_NEED_LENGTH, null),
    BARCODE(EasyBarCode.class, "二维码", FieldType.STRING, 300, null, false, true),
    N2NREFERENCE(EasyN2NReference.class, "多引用", FieldType.REFERENCE_LIST, -1, null),
    LOCATION(EasyLocation.class, "位置", FieldType.STRING, 100, null),
    SIGN(EasySign.class, "签名", FieldType.TEXT, 32767, null, false, false),

    // 内部

    ID(EasyID.class, "主键", FieldType.PRIMARY, -1, null, false, true),
    STATE(EasyState.class, "状态", FieldType.SMALL_INT, -1, null),
    ANYREFERENCE(EasyAnyReference.class, "任意引用", FieldType.ANY_REFERENCE, -1, null, false, true),

    ;

    // --

    private final Class<? extends EasyField> easyClass;
    private final String displayName;
    private final Type fieldType;
    private final int maxLength;
    private final String defaultFormat;

    private final boolean importable;
    private final boolean exportable;

    DisplayType(Class<? extends EasyField> easyClass, String displayName, Type fieldType, int maxLength, String defaultFormat) {
        this(easyClass, displayName, fieldType, maxLength, defaultFormat, true, true);
    }

    DisplayType(Class<? extends EasyField> easyClass, String displayName, Type fieldType, int maxLength, String defaultFormat,
                boolean importable, boolean exportable) {
        this.easyClass = easyClass;
        this.displayName = displayName;
        this.fieldType = fieldType;
        this.maxLength = maxLength;
        this.defaultFormat = defaultFormat;
        this.importable = importable;
        this.exportable = exportable;
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

    public boolean isImportable() {
        return importable;
    }

    public boolean isExportable() {
        return exportable;
    }

    Class<? extends EasyField> getEasyClass() {
        return easyClass;
    }

    @Override
    public String toString() {
        return getDisplayName() + " (" + name().toUpperCase() + ")";
    }
}
