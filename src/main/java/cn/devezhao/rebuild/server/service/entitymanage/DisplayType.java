/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.server.service.entitymanage;

import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;

/**
 * @author zhaofang123@gmail.com
 * @since 05/18/2018
 */
public enum DisplayType {
	
	NUMBER("数字", FieldType.LONG),
	DECIMAL("货币", FieldType.DECIMAL),
	DATE("日期", FieldType.DATE),
	DATETIME("日期时间", FieldType.TIMESTAMP),
	TEXT("文本", FieldType.STRING),
	EMAIL("邮箱", FieldType.STRING),
	URL("链接", FieldType.STRING),
	PHONE("电话", FieldType.STRING),
	IMAGE("图片", FieldType.STRING),
	FILE("附件", FieldType.STRING),
	PICKLIST("列表", FieldType.REFERENCE),
	REFERENCE("引用", FieldType.REFERENCE),
	
	ID("主键", FieldType.PRIMARY);
	
	// --
	
	private String displayName;
	private Type fieldType;
	
	private DisplayType(String displayName, Type fieldType) {
		this.displayName = displayName;
		this.fieldType = fieldType;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public Type getFieldType() {
		return fieldType;
	}
	
	@Override
	public String toString() {
		return getDisplayName() + " (" + name().toUpperCase() + ")";
	}
}
