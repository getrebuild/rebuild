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

package com.rebuild.server.service.entitymanage;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.metadata.impl.EntityImpl;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/04/2018
 */
public class UnsafeEntity extends EntityImpl {
	private static final long serialVersionUID = 2107073554299141281L;

	public UnsafeEntity(String entityName, String physicalName, String entityLabel, int typeCode, String nameField) {
		super(entityName, physicalName, entityLabel, typeCode, nameField);
	}
	
	@Override
	public void addField(Field field) {
		super.addField(field);
	}
}
