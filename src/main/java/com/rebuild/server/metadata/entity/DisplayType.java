/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.metadata.entity;

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
	TEXT("文本", FieldType.STRING, 300),
	NTEXT("多行文本", FieldType.TEXT, 6000),
	EMAIL("邮箱", FieldType.STRING, 100),
	URL("链接", FieldType.STRING, 300),
	PHONE("电话", FieldType.STRING, 20),
	SERIES("自动编号", FieldType.STRING, 40, "{YYYYMMDD}-{0000}"),
	IMAGE("图片", FieldType.STRING, 700),
	FILE("附件", FieldType.STRING, 700),
	PICKLIST("列表", FieldType.REFERENCE, -1),
	CLASSIFICATION("分类", FieldType.REFERENCE, -1),
	REFERENCE("引用", FieldType.REFERENCE, -1),
    AVATAR("头像", FieldType.STRING, 300),
    MULTISELECT("多选", FieldType.LONG, -1),

    BOOL("布尔", FieldType.BOOL, -1),
    STATE("状态", FieldType.SMALL_INT, -1),

    // 内部用/未开放,
    ID("主键", FieldType.PRIMARY, -1),
    ANYREFERENCE("任意引用", FieldType.ANY_REFERENCE, -1),
    LOCATION("位置", FieldType.STRING, 70),

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
