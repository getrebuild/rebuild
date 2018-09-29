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

package com.rebuild.server.entityhub;

import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;

/**
 * @author zhaofang123@gmail.com
 * @since 05/18/2018
 */
public enum DisplayType {
	
	NUMBER("数字", FieldType.LONG, "##,###"),
	DECIMAL("货币", FieldType.DECIMAL, "##,##0.00"),
	
	DATE("日期", FieldType.DATE, "yyyy-MM-dd"),
	DATETIME("日期时间", FieldType.TIMESTAMP, "yyyy-MM-dd HH:mm"),
	
	TEXT("文本", FieldType.STRING, null),
	NTEXT("超大文本", FieldType.TEXT, null),
	EMAIL("邮箱", FieldType.STRING, null),
	URL("链接", FieldType.STRING, null),
	PHONE("电话", FieldType.STRING, null),

	IMAGE("图片", FieldType.STRING, null),
	FILE("附件", FieldType.STRING, null),
	
	PICKLIST("列表", FieldType.REFERENCE, null),
	REFERENCE("引用", FieldType.REFERENCE, null),
	ID("主键", FieldType.PRIMARY, null),
	
	LOCATION("位置", FieldType.STRING, null),
	
	_BOOL("布尔", FieldType.BOOL, null);
	
	// --
	
	private String displayName;
	private Type fieldType;
	private String defaultFormat;
	
	private DisplayType(String displayName, Type fieldType, String defaultFormat) {
		this.displayName = displayName;
		this.fieldType = fieldType;
		this.defaultFormat = defaultFormat;
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
	
	@Override
	public String toString() {
		return getDisplayName() + " (" + name().toUpperCase() + ")";
	}
}
