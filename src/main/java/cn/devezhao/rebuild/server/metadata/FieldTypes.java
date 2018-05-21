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

package cn.devezhao.rebuild.server.metadata;

import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 05/18/2018
 */
public class FieldTypes {
	
	// 文本
	public static final Type TEXT = FieldType.STRING;
	
	// 超大文本
	public static final Type NTEXT = FieldType.NTEXT;
	
	// 数字/整数/浮点数
	public static final Type NUMBER = FieldType.DOUBLE;
	
	// 下拉列表
	public static final Type PICKLIST = FieldType.INT;
	
	// 日期/日期时间
	public static final Type DATETIME = FieldType.TIMESTAMP;
	
	// QQ
	public static final Type QQ = FieldType.STRING;
	
	// 邮箱
	public static final Type EMAIL = FieldType.STRING;
	
	// 链接
	public static final Type URL = FieldType.STRING;
	
	// 位置
	public static final Type LOCATION = FieldType.STRING;
	
	// 图片
	public static final Type IMAGE = FieldType.STRING;
}
