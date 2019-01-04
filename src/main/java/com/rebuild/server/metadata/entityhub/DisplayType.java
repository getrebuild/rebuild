/*
rebuild - Building your system freely.
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

package com.rebuild.server.metadata.entityhub;

import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;

/**
 * @author zhaofang123@gmail.com
 * @since 05/18/2018
 */
public enum DisplayType {
	
	NUMBER("数字", FieldType.LONG, -1, "##,###"),
	DECIMAL("货币", FieldType.DECIMAL, -1, "##,##0.00"),
	DATE("日期", FieldType.DATE, -1, "yyyy-MM-dd"),
	DATETIME("日期时间", FieldType.TIMESTAMP, -1, "yyyy-MM-dd HH:mm:ss"),
	TEXT("文本", FieldType.STRING, 300, null),
	NTEXT("大文本", FieldType.TEXT, 6000, null),
	EMAIL("邮箱", FieldType.STRING, 100, null),
	URL("链接", FieldType.STRING, 300, null),
	PHONE("电话", FieldType.STRING, 20, null),
	SERIES("自动编号", FieldType.STRING, 40, "{YYYYMMDD}-{0000}"),
	IMAGE("图片", FieldType.STRING, 700, null),
	FILE("附件", FieldType.STRING, 700, null),
	PICKLIST("列表", FieldType.REFERENCE, -1, null),
	REFERENCE("引用", FieldType.REFERENCE, -1, null),
	
	// 待启用/内部用
	ID("主键", FieldType.PRIMARY, -1, null),
	ANYREFERENCE("任意引用", FieldType.ANY_REFERENCE, -1, null),
	BOOL("布尔", FieldType.BOOL, -1, null),
	LOCATION("位置", FieldType.STRING, 70, null),
	
	;
	
	// --
	
	private String displayName;
	private Type fieldType;
	private String defaultFormat;
	private int maxLength;
	
	private DisplayType(String displayName, Type fieldType) {
		this.displayName = displayName;
		this.fieldType = fieldType;
	}
	
	private DisplayType(String displayName, Type fieldType, int maxLength, String defaultFormat) {
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
