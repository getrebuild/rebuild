/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;

/**
 * @author zhaofang123@gmail.com
 * @since 05/18/2018
 */
public enum DisplayType {

    NUMBER("整数", FieldType.LONG, -1, "##,###"),
    DECIMAL("货币", FieldType.DECIMAL, -1, "##,##0.00"),
    DATE("日期", FieldType.DATE, -1, "yyyy-MM-dd"),
    DATETIME("日期时间", FieldType.TIMESTAMP, -1, "yyyy-MM-dd HH:mm:ss"),
    TEXT("文本", FieldType.STRING, 200),
    NTEXT("多行文本", FieldType.TEXT, 3000),
    EMAIL("邮箱", FieldType.STRING, 100),
    URL("链接", FieldType.STRING, 300),
    PHONE("电话", FieldType.STRING, 40),
    SERIES("自动编号", FieldType.STRING, 40, "{YYYYMMDD}-{0000}"),
    IMAGE("图片", FieldType.STRING, 700),
    FILE("附件", FieldType.STRING, 700),
    PICKLIST("列表", FieldType.REFERENCE, -1),
    CLASSIFICATION("分类", FieldType.REFERENCE, -1),
    REFERENCE("引用", FieldType.REFERENCE, -1),
    AVATAR("头像", FieldType.STRING, 100),
    MULTISELECT("多选", FieldType.LONG, -1),
    BOOL("布尔", FieldType.BOOL, -1),
    BARCODE("条形码", FieldType.TEXT, 300),

    // 内部

    ID("主键", FieldType.PRIMARY, -1),
    LOCATION("位置", FieldType.STRING, 70),
    STATE("状态", FieldType.SMALL_INT, -1),
    ANYREFERENCE("任意引用", FieldType.ANY_REFERENCE, -1),
    N2NREFERENCE("多对多引用", FieldType.REFERENCE_LIST, -1),

    ;

    // --

    private String displayName;
    private Type fieldType;
    private String defaultFormat;
    private int maxLength;

    DisplayType(String displayName, Type fieldType, int maxLength) {
        this(displayName, fieldType, maxLength, null);
    }

    DisplayType(String displayName, Type fieldType, int maxLength, String defaultFormat) {
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

    public String getDefaultFormat() {
        return defaultFormat;
    }

    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public String toString() {
        return getDisplayName() + " (" + name().toUpperCase() + ")";
    }
}
